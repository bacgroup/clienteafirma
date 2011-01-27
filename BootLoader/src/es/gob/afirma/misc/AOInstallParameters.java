/*
 * Este fichero forma parte del Cliente @firma. 
 * El Cliente @firma es un applet de libre distribuci�n cuyo c�digo fuente puede ser consultado
 * y descargado desde www.ctt.map.es.
 * Copyright 2009,2010 Gobierno de Espa�a
 * Este fichero se distribuye bajo las licencias EUPL versi�n 1.1  y GPL versi�n 3, o superiores, seg�n las
 * condiciones que figuran en el fichero 'LICENSE.txt' que se acompa�a.  Si se   distribuyera este 
 * fichero individualmente, deben incluirse aqu� las condiciones expresadas all�.
 */

package es.gob.afirma.misc;

import java.io.File;
import java.security.AccessController;
import java.util.logging.Logger;

/**
 * Constantes de utilidad en toda la aplicaci&oacute;n.
 * @version 0.3
 */
public final class AOInstallParameters {
		
	/** Directorio de usuario. */
	public static final String USER_HOME;
	static {
	    USER_HOME = AccessController.doPrivileged(new java.security.PrivilegedAction<String>() {
	        public String run() {
	            try {
	                return System.getProperty("user.home") + File.separator; //$NON-NLS-1$
	            } 
	            catch (final Throwable e) {
	                Logger.getLogger("es.gob.afirma").severe("No ha podido determinarse el directorio de usuario para la configuracion del cliente"); //$NON-NLS-1$ //$NON-NLS-2$
	                return ""; //$NON-NLS-1$
	            }
	        }
	    });
	}
	
	/** Directorio de instalacion por defecto de las librer&iacute;as del applet. */
	public static final String DEFAULT_INSTALLATION_DIR = "afirma.5"; //$NON-NLS-1$
	
	/** Directorio de instalaci&oacute;n. */
	private static String installationDir = DEFAULT_INSTALLATION_DIR;
	
	/**
	 * Establece el nombre del directorio de instalacion para poder parametrizar el resto de directorios.
	 * Por ejemplo, si el directorio de instalacion fuese: "C:/Documents and Settings/User1/afirma.5"<br/>
	 * deber&iacute;amos utilizar como par&aacute;metro "afirma.5". Si se establece null, se tomara el
	 * directorio de instalacion por defecto.
	 * @param installationDirectory Directorio de instalaci&oacute;n del applet.
	 */
	public static void setInstallationDirectory(final String installationDirectory) {
		if(installationDirectory != null) {
			installationDir = installationDirectory;
		} 
		else {
			installationDir = DEFAULT_INSTALLATION_DIR;
		}
	}
	
	//************************************************************
	//************* DIRECTORIOS DE INSTALACION *******************
	//************************************************************
	
	/**
	 * Recupera el directorio de instalaci&oacute;n del cliente (s&oacute;lo el nombre del directorio).
	 * @return Directorio de instalaci&oacute;n.
	 */
	public static String getInstallationDirectory() {
		return installationDir;
	}
	
	/**
	 * Recupera el directorio de instalaci&oacute;n del cliente con el path completo.
	 * @return Directorio de instalaci&oacute;n.
	 */
	public static String getHomeApplication() {
		return USER_HOME + installationDir;
	}
		
	   /** Preguntar al usuario sobre la acci&oacute;n a realizar. */
    public final static int ACTION_ASK = 1;
    
    /** Respetar instalaciones antiguas del cliente. */
    public final static int ACTION_RESPECT = 2;
    
    /** Eliminar instalaciones antiguas del cliente. */
    public final static int ACTION_DELETE = 3;
    
    /** Acci&oacute;n a realizar con respecto a las versiones antiguas encontradas del cliente. */
    public int oldVersionsAction = ACTION_ASK;
}
