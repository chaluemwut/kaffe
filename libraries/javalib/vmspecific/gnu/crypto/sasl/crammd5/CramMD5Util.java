package gnu.crypto.sasl.crammd5;

// ----------------------------------------------------------------------------
// $Id: CramMD5Util.java,v 1.1 2005/10/19 20:15:47 guilhem Exp $
//
// Copyright (C) 2003 Free Software Foundation, Inc.
//
// This file is part of GNU Crypto.
//
// GNU Crypto is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2, or (at your option)
// any later version.
//
// GNU Crypto is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; see the file COPYING.  If not, write to the
//
//    Free Software Foundation Inc.,
//    51 Franklin Street, Fifth Floor,
//    Boston, MA 02110-1301
//    USA
//
// Linking this library statically or dynamically with other modules is
// making a combined work based on this library.  Thus, the terms and
// conditions of the GNU General Public License cover the whole
// combination.
//
// As a special exception, the copyright holders of this library give
// you permission to link this library with independent modules to
// produce an executable, regardless of the license terms of these
// independent modules, and to copy and distribute the resulting
// executable under terms of your choice, provided that you also meet,
// for each linked independent module, the terms and conditions of the
// license of that module.  An independent module is a module which is
// not derived from or based on this library.  If you modify this
// library, you may extend this exception to your version of the
// library, but you are not obligated to do so.  If you do not wish to
// do so, delete this exception statement from your version.
// ----------------------------------------------------------------------------

import gnu.crypto.Registry;
import gnu.crypto.mac.HMacFactory;
import gnu.crypto.mac.IMac;
import gnu.crypto.util.Util;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.util.HashMap;

import javax.security.sasl.SaslException;

/**
 * A package-private CRAM-MD5-specific utility class.
 *
 * @version $Revision: 1.1 $
 */
class CramMD5Util {

   // Constants and variables
   // -------------------------------------------------------------------------

   // Constructor(s)
   // -------------------------------------------------------------------------

   private CramMD5Util() {
      super();
   }

   // Class methods
   // -------------------------------------------------------------------------

   static byte[] createMsgID() throws SaslException {
      // <process-ID.clock@hostname>
      final String encoded;
      try {
         encoded = Util.toBase64(Thread.currentThread().getName().getBytes("UTF-8"));
      } catch (UnsupportedEncodingException x) {
         throw new SaslException("createMsgID()", x);
      }
      String hostname = "localhost";
      try {
         hostname = InetAddress.getLocalHost().getHostAddress();
      } catch (UnknownHostException ignored) {
      }

      final byte[] result;
      try {
         result = new StringBuffer()
               .append("<").append(encoded.substring(0, encoded.length()))
               .append(".").append(String.valueOf(System.currentTimeMillis()))
               .append("@").append(hostname).append(">")
               .toString()
               .getBytes("UTF-8");
      } catch (UnsupportedEncodingException x) {
         throw new SaslException("createMsgID()", x);
      }
      return result;
   }

   static byte[] createHMac(final char[] passwd, final byte[] data)
   throws InvalidKeyException, SaslException {
      final IMac mac = HMacFactory
            .getInstance(Registry.HMAC_NAME_PREFIX + Registry.MD5_HASH);
      final HashMap map = new HashMap();
      final byte[] km;
      try {
         km = new String(passwd).getBytes("UTF-8");
      } catch (UnsupportedEncodingException x) {
         throw new SaslException("createHMac()", x);
      }
      map.put(IMac.MAC_KEY_MATERIAL, km);
      mac.init(map);
      mac.update(data, 0, data.length);
      return mac.digest();
   }
}
