/* gnu.classpath.tools.gjdoc.GjdocPackageDoc
   Copyright (C) 2001 Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.
 
GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
02111-1307 USA. */

package gnu.classpath.tools.gjdoc;

import java.io.File;
import com.sun.javadoc.PackageDoc;

/**
 *  Extension of the PackageDoc interface which additionally provides
 *  the directory the package's source files are located in.
 *
 *  @author Julian Scheid
 */
public interface GjdocPackageDoc extends PackageDoc
{
   /**
    *  Returns the directory this package's source files are located
    *  in.
    */
   public File packageDirectory();
}