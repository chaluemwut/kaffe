/*
 * $Id: DomIterator.java,v 1.1 2002/12/03 01:27:56 dalibor Exp $
 * Copyright (C) 1999-2001 David Brownell
 * 
 * This file is part of GNU JAXP, a library.
 *
 * GNU JAXP is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * GNU JAXP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * As a special exception, if you link this library with other files to
 * produce an executable, this library does not by itself cause the
 * resulting executable to be covered by the GNU General Public License.
 * This exception does not however invalidate any other reasons why the
 * executable file might be covered by the GNU General Public License. 
 */

package gnu.xml.dom;

import java.util.Vector;

import org.w3c.dom.*;
import org.w3c.dom.events.*;
import org.w3c.dom.traversal.*;


// $Id: DomIterator.java,v 1.1 2002/12/03 01:27:56 dalibor Exp $

/**
 * <p> "NodeIterator" implementation, usable with any L2 DOM which
 * supports MutationEvents. </p>
 *
 * @author David Brownell 
 * @version $Date: 2002/12/03 01:27:56 $
 */
final public class DomIterator implements NodeIterator, EventListener
{
    private Node		reference;
    private boolean		right;
    private boolean		done;

    private final Node		root;
    private final int		whatToShow;
    private final NodeFilter	filter;
    private final boolean	expandEntityReferences;


    /**
     * Constructs and initializes an iterator.
     */
    protected DomIterator (
	Node		root,
	int		whatToShow,
	NodeFilter	filter,
	boolean		entityReferenceExpansion
    ) {
	if (!root.isSupported ("MutationEvents", "2.0")) 
	    throw new DomEx (DomEx.NOT_SUPPORTED_ERR,
		    "Iterator needs mutation events", root, 0);
	
	this.root = root;
	this.whatToShow = whatToShow;
	this.filter = filter;
	this.expandEntityReferences = entityReferenceExpansion;

	// start condition:  going right, seen nothing yet.
	reference = null;
	right = true;

	EventTarget	target = (EventTarget) root;
	target.addEventListener ("DOMNodeRemoved", this, false);
    }


    /**
     * <b>DOM L2</b>
     * Flags the iterator as done, unregistering its event listener so
     * that the iterator can be garbage collected without relying on weak
     * references (a "Java 2" feature) in the event subsystem.
     */
    public void detach ()
    {
	EventTarget	target = (EventTarget) root;
	target.removeEventListener ("DOMNodeRemoved", this, false);
	done = true;
    }


    /**
     * <b>DOM L2</b>
     * Returns the flag controlling whether iteration descends
     * through entity references.
     */
    public boolean getExpandEntityReferences ()
    {
	return expandEntityReferences;
    }

    
    /**
     * <b>DOM L2</b>
     * Returns the filter provided during construction.
     */
    public NodeFilter getFilter ()
    {
	return filter;
    }

    
    /**
     * <b>DOM L2</b>
     * Returns the root of the tree this is iterating through.
     */
    public Node getRoot ()
    {
	return root;
    }

    
    /**
     * <b>DOM L2</b>
     * Returns the mask of flags provided during construction.
     */
    public int getWhatToShow ()
    {
	return whatToShow;
    }

    
    /**
     * <b>DOM L2</b>
     * Returns the next node in a forward iteration, masked and filtered.
     * Note that the node may be read-only due to entity expansions.
     * A null return indicates the iteration is complete, but may still
     * be processed backwards.
     */
    public Node nextNode ()
    {
	if (done)
	    throw new DomEx (DomEx.INVALID_STATE_ERR);
	right = true;
	return walk (true);
    }


    /**
     * <b>DOM L2</b>
     * Returns the next node in a backward iteration, masked and filtered.
     * Note that the node may be read-only due to entity expansions.
     * A null return indicates the iteration is complete, but may still
     * be processed forwards.
     */
    public Node previousNode ()
    {
	if (done)
	    throw new DomEx (DomEx.INVALID_STATE_ERR);
	Node previous = reference;
	right = false;
	walk (false);
	return previous;
    }

    private boolean shouldShow (Node node)
    // raises Runtime exceptions indirectly, via acceptNode()
    {
	if ((whatToShow & (1 << (node.getNodeType () - 1))) == 0)
	    return false;
	if (filter == null)
	    return true;
	return filter.acceptNode (node) == NodeFilter.FILTER_ACCEPT;
    }

    //
    // scenario:  root = 1, sequence = 1 2 ... 3 4
    // forward walk: 1 2 ... 3 4 null
    // then backward: 4 3 ... 2 1 null
    //
    // At the leftmost end, "previous" == null
    // At the rightmost end, "previous" == 4
    //
    // The current draft spec really seem to make no sense re the
    // role of the reference node, so what it says is ignored here.
    //
    private Node walk (boolean forward)
    {
	Node	here = reference;

	while ((here = successor (here, forward)) != null
		&& !shouldShow (here))
	    continue;
	if (here != null || !forward)
	    reference = here;
	return here;
    }

    private boolean isLeaf (Node here)
    {
	boolean leaf = !here.hasChildNodes ();
	if (!leaf && !expandEntityReferences)
	    leaf = (here.getNodeType () == Node.ENTITY_REFERENCE_NODE);
	return leaf;
    }
    
    //
    // Returns the immediate successor in a forward (or backward)
    // document order walk, sans filtering ... except that it knows
    // how to stop, returning null when done.  This is a depth first
    // preorder traversal when run in the forward direction.
    //
    private Node successor (Node here, boolean forward)
    {
	Node	next;

	// the "leftmost" end is funky
	if (here == null)
	    return forward ? root : null;

	//
	// Forward, this is preorder: children before siblings.
	// Backward, it's postorder: we saw the children already.
	//
	if (forward && !isLeaf (here))
	    return here.getFirstChild ();

	//
	// Siblings ... if forward, we visit them, if backwards
	// we visit their children first.
	//
	if (forward) {
	    if ((next = here.getNextSibling ()) != null)
		return next;
	} else if ((next = here.getPreviousSibling ()) != null) {
	    if (isLeaf (next))
		return next;
	    next = next.getLastChild ();
	    while (!isLeaf (next))
		next = next.getLastChild ();
	    return next;
	}
	
	//
	// We can't go down or lateral -- it's up, then.  The logic is
	// the converse of what's above:  backwards is easy (the parent
	// is next), forwards isn't.
	//
	next = here.getParentNode ();
	if (!forward)
	    return next;

	Node temp = null;
	while (next != null
		&& next != root
		&& (temp = next.getNextSibling ()) == null)
	    next = next.getParentNode ();
	if (next == root)
	    return null;
	return temp;
    }


    /**
     * Not for public use.  This lets the iterator know when its
     * reference node will be removed from the tree, so that a new
     * one may be selected.
     *
     * <p> This version works by watching removal events as they
     * bubble up.  So, don't prevent them from bubbling.
     */
    public void handleEvent (Event e)
    {
	MutationEvent	event;
	Node		ancestor, removed;

	if (reference == null
		|| !"DOMNodeRemoved".equals (e.getType ())
		|| e.getEventPhase () != Event.BUBBLING_PHASE)
	    return;

	event = (MutationEvent) e;
	removed = (Node) event.getTarget ();

	// See if the removal will cause trouble for this iterator
	// by being the reference node or an ancestor of it.
	for (ancestor = reference;
		ancestor != null && ancestor != root;
		ancestor = ancestor.getParentNode ()) {
	    if (ancestor == removed)
		break;
	}
	if (ancestor != removed)
	    return;

	// OK, it'll cause trouble.  We want to make the "next"
	// node in our current traversal direction seem right.
	// So we pick the nearest node that's not getting removed,
	// but go in the _opposite_ direction from our current
	// traversal ... so the "next" doesn't skip anything.
	Node	candidate;

search:
	while ((candidate = walk (!right)) != null) {
	    for (ancestor = candidate;
		    ancestor != null && ancestor != root;
		    ancestor = ancestor.getParentNode ()) {
		if (ancestor == removed)
		    continue search;
	    }
	    return;
	}
	
	// The current DOM WD talks about a special case here;
	// I've not yet seen it.
    }
}
