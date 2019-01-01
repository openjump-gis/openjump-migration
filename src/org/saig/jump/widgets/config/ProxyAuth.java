/* 
 * Kosmo - Sistema Abierto de Informaci�n Geogr�fica
 * Kosmo - Open Geographical Information System
 *
 * http://www.saig.es
 * (C) 2009, SAIG S.L.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation;
 * version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * For more information, contact:
 * 
 * Sistemas Abiertos de Informaci�n Geogr�fica, S.L.
 * Avnda. Rep�blica Argentina, 28
 * Edificio Domocenter Planta 2� Oficina 7
 * C.P.: 41930 - Bormujos (Sevilla)
 * Espa�a / Spain
 *
 * Tel�fono / Phone Number
 * +34 954 788876
 * 
 * Correo electr�nico / Email
 * info@saig.es
 *
 */
package org.saig.jump.widgets.config;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * Authentification for net requests
 * <p>
 * </p>
 * 
 * @author Sergio Ba�os Calvo
 * @since 1.3
 */
public class ProxyAuth extends Authenticator {
    
    /** */
    private PasswordAuthentication auth;

    /**
     * 
     * 
     * @param user
     * @param pass
     */
    public ProxyAuth( String user, String pass ) {
        auth = new PasswordAuthentication(user, pass.toCharArray());
    }

    /**
     * 
     */
    protected PasswordAuthentication getPasswordAuthentication() {
        return auth;
    }
}
