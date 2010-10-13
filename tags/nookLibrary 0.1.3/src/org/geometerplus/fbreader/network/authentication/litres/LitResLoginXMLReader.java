/*
 * Copyright (C) 2010 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.fbreader.network.authentication.litres;

import org.geometerplus.fbreader.network.NetworkErrors;
import org.geometerplus.zlibrary.core.xml.ZLStringMap;


class LitResLoginXMLReader extends LitResAuthenticationXMLReader {

    private static final String TAG_AUTHORIZATION_OK = "catalit-authorization-ok";
    private static final String TAG_AUTHORIZATION_FAILED = "catalit-authorization-failed";

    public String FirstName;
    public String LastName;
    public String Sid;

    public LitResLoginXMLReader(String hostName) {
        super(hostName);
    }

    @Override
    public boolean startElementHandler(String tag, ZLStringMap attributes) {
        tag = tag.toLowerCase().intern();
        if (TAG_AUTHORIZATION_FAILED == tag) {
            setErrorCode(NetworkErrors.ERROR_AUTHENTICATION_FAILED);
        } else if (TAG_AUTHORIZATION_OK == tag) {
            FirstName = attributes.getValue("first-name");
            LastName = attributes.getValue("first-name");
            Sid = attributes.getValue("sid");
        } else {
            setErrorCode(NetworkErrors.ERROR_SOMETHING_WRONG, HostName);
        }
        return true;
    }
}