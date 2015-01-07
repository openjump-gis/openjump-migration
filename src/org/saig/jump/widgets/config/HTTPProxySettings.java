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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.saig.core.crypt.CryptManager;
import org.saig.core.crypt.CryptManagerException;
import org.saig.core.crypt.CryptManagerFactory;

/**
 * Wrapper to the HTTP Proxy settings parameters 
 * <p>
 *
 * </p>
 * @author Sergio Ba�os Calvo
 * @since 2.0
 */
public class HTTPProxySettings {
    
    /** Log */
    private final static Logger LOGGER = Logger.getLogger(HTTPProxySettings.class);
    
    private boolean enabled = true;
    
    private String host;
    
    private String port;
    
    private String userName;
    
    private String password;
    
    private String directConnectionTo;
    
    private CryptManager manager;
    
    /**
     * 
     */
    public HTTPProxySettings()
    {
        try {
            manager = CryptManagerFactory.getManager(CryptManagerFactory.PASSWORD_BASED_ENCRYPTION);
        } catch (CryptManagerException e) {
            LOGGER.error(e);
        }
    }

    public HTTPProxySettings(boolean enabled) {
      this.enabled = enabled;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost( String host ) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort( String port ) {
        this.port = port;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName( String userName ) {
        this.userName = userName;
    }

    public String getPassword() {
        if(StringUtils.isEmpty(password)) {
            return "";
        }
        return password;
    }

    public void setPassword( String password ) {
        this.password = password;
    }

    /**
     * @param directConnectionTo The directConnectionTo to set.
     */
    public void setDirectConnectionTo( String directConnectionTo ) {
        this.directConnectionTo = directConnectionTo;
    }

    /**
     * @return Returns the directConnectionTo.
     */
    public String getDirectConnectionTo() {
        return directConnectionTo;
    }

    /**
     *
     * @return
     */
    public String getEncryptedPassword() {
        String encryptedPassword = password;
        if(manager != null)
        {
            try {
                encryptedPassword = manager.encrypt(password);
            } catch (Exception e) {
                LOGGER.error(e);
            }
        }
        return encryptedPassword;
    }

    /**
     *
     * @param encryptedPassword
     */
    public void setEncryptedPassword( String encryptedPassword ) {
        try {
            password = manager.decrypt(encryptedPassword);
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }
}
