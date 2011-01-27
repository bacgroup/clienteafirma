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

import java.awt.Component;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.swing.ProgressMonitorInputStream;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import es.gob.afirma.exceptions.AOException;
import es.gob.afirma.misc.mozilla.utils.NSPreferences;

/** 
 * M&eacute;todos generales de utilidad para toda la aplicaci&oacute;n.
 * @version 0.3 
 */
public final class AOUtil {

	private AOUtil() {}

	private static final String[] SUPPORTED_URI_SCHEMES = new String[] {
		"http",
		"https",
		"file"
	};

	/**
	 * Crea una URI a partir de un nombre de fichero local o una URL.
	 * @param filename Nombre del fichero local o URL
	 * @return URI (<code>file://</code>) del fichero local o URL
	 * @throws AOException cuando ocurre cualquier problema creando la URI
	 */
	public final static URI createURI(String filename) throws AOException {

		if (filename == null)
			throw new AOException("No se puede crear una URI a partir de un nulo");

		filename = filename.trim();
		
		if ("".equals(filename))
			throw new AOException("La URI no puede ser una cadena vacia");
		
		
		// Cambiamos los caracteres Windows
		filename = filename.replace('\\', '/');

		// Cambiamos los espacios por %20
		filename = filename.replace(" ", "%20");

		URI uri;
		try {
			uri = new URI(filename);
		}
		catch(final Throwable e) {
			throw new AOException("Formato de URI (" + filename + ") incorrecto: " + e);
		}

		// Comprobamos si es un esquema soportado
		final String scheme = uri.getScheme();
		for(int i=0; i<SUPPORTED_URI_SCHEMES.length; i++)
			if (SUPPORTED_URI_SCHEMES[i].equals(scheme))
				return uri;

		// Si el esquema es nulo, aun puede ser un nombre de fichero valido
		if(scheme == null)
			return createURI("file://" + filename);

		// Miramos si el esquema es una letra, en cuyo caso seguro que es una
		// unidad de Windows ("C:", "D:", etc.), y le anado el file://
		if (scheme.length() == 1 && Character.isLetter((char) scheme.getBytes()[0]))
			return createURI("file://" + filename);		

		throw new AOException("Formato de URI valido pero no soportado '" + filename + "'");

	}

//	/**
//	 * Obtiene el flujo de entrada de un fichero (para su lectura) a partir de su URI.
//	 * @param uri URI del fichero a leer
//	 * @param c Componente grafico que invoca al m&eacute;todo (para la modalidad
//	 *          del di&aacute;logo de progreso)
//	 * @param waitDialog Indica si debe mostrarse o no un di&aacute;logo de progreso de la carga y lectura
//	 * @return Flujo de entrada hacia el contenido del fichero
//	 * @throws FileNotFoundException Cuando no se encuentra el fichero indicado
//	 * @throws AOException Cuando ocurre cualquier problema obteniendo el flujo
//	 */
//	public final static InputStream loadFile(final URI uri, final Component c, final boolean waitDialog) throws FileNotFoundException, AOException {
//		
//		// Cuidado: Repinta mal el dialogo de espera, hay que tratar con hilos nuevos
//		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4209604
//
//		if (uri == null) throw new NullPointerException("Se ha pedido el contenido de una URI nula");
//
//		javax.swing.ProgressMonitor pm = null;
//		
//		if (uri.getScheme().equals("file")) {
//			// Es un fichero en disco. Las URL de Java no soportan file://, con
//			// lo que hay que diferenciarlo a mano
//			try {
//				// Retiramos el "file://" de la uri
//				String path = uri.getSchemeSpecificPart();
//				if(path.startsWith("//")) path = path.substring(2);
//				// Cuidado, el ProgressMonitor no se entera del tamano de los ficheros grandes:
//				// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6445283
//				if (waitDialog) return new BufferedInputStream(new ProgressMonitorInputStream(
//					c,
//					"Leyendo " + path,
//					new FileInputStream(new File(path))
//				));
//				
//				return new BufferedInputStream(new FileInputStream(new File(path)));
//			}
//			catch(final NullPointerException e) {
//				throw e;
//			}
//			catch(final FileNotFoundException e) {
//				throw e;
//			}
//			catch(final Throwable e) {
//			    e.printStackTrace();
//			    throw new AOException("Ocurrio un error intentando abrir un archivo en almacenamiento local: " + e);
//			}
//		}
//		
//		// Es una URL
//		InputStream tmpStream;
//		try {
//			if (waitDialog) {
//				final ProgressMonitorInputStream pmis = new ProgressMonitorInputStream(
//					c,
//					"Leyendo " + uri.toURL().toString(),
//					uri.toURL().openStream()
//				);
//				pm = pmis.getProgressMonitor(); 
////					pm.setMillisToDecideToPopup(0);
////					pm.setMillisToPopup(0);
//				
//				// Las URL pocas veces informan del tamano del fichero, asi que ponemos un valor alto
//				// por defecto para segurarnos de que el dialogo se muestra
//				pm.setMaximum(10000000);
//				
//				tmpStream = new BufferedInputStream(pmis);
//			}
//			else tmpStream = new BufferedInputStream(uri.toURL().openStream());
//		}
//		catch(final Throwable e) {
//			if (pm != null) pm.close();
//			throw new AOException(
//				"Ocurrio un error intentando abrir la URI '" + uri.toASCIIString() + "' como URL: " + e
//			);
//		}
//		// Las firmas via URL fallan en la descarga por temas de Sun, asi que descargamos primero
//		// y devolvemos un Stream contra un array de bytes
//		final byte[] tmpBuffer;
//		try {
//			tmpBuffer = getDataFromInputStream(tmpStream);
//		}
//		catch(final Throwable e) {
//			if (pm != null) pm.close();
//			throw new AOException("Error leyendo el fichero remoto '" + uri.toString() + "': " + e);
//		}
//		
//		// Hay que cerrar el ProgressMonitor a mano:
//		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4174850
//		if (pm != null) pm.close();
//
////		Logger.getLogger("es.gob.afirma").info(
////			"Leido fichero de " + tmpBuffer.length + " bytes:\n" + new String(tmpBuffer)
////		);
//		return new java.io.ByteArrayInputStream(tmpBuffer);
//	}

	/**
	 * Obtiene el flujo de entrada de un fichero (para su lectura) a partir de su URI.
	 * @param uri URI del fichero a leer
	 * @param c Componente grafico que invoca al m&eacute;todo (para la modalidad
	 *          del di&aacute;logo de progreso)
	 * @param waitDialog Indica si debe mostrarse o no un di&aacute;logo de progreso de la carga y lectura
	 * @return Flujo de entrada hacia el contenido del fichero
	 * @throws FileNotFoundException Cuando no se encuentra el fichero indicado
	 * @throws AOException Cuando ocurre cualquier problema obteniendo el flujo
	 */
	public final static InputStream loadFile(final URI uri, final Component c, final boolean waitDialog) throws FileNotFoundException, AOException {
		
		// Cuidado: Repinta mal el dialogo de espera, hay que tratar con hilos nuevos
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4209604

		if (uri == null) throw new NullPointerException("Se ha pedido el contenido de una URI nula");

		javax.swing.ProgressMonitor pm = null;
		
		if (uri.getScheme().equals("file")) {
			// Es un fichero en disco. Las URL de Java no soportan file://, con
			// lo que hay que diferenciarlo a mano
			try {
				// Retiramos el "file://" de la uri
				String path = uri.getSchemeSpecificPart();
				if(path.startsWith("//")) path = path.substring(2);
				// Cuidado, el ProgressMonitor no se entera del tamano de los ficheros grandes:
				// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6445283
				if (waitDialog) return new ProgressMonitorInputStream(
					c,
					"Leyendo " + path,
					new FileInputStream(new File(path))
				);
				
				return new FileInputStream(new File(path));
			}
			catch(final NullPointerException e) {
				throw e;
			}
			catch(final FileNotFoundException e) {
				throw e;
			}
			catch(final Throwable e) {
			    e.printStackTrace();
			    throw new AOException("Ocurrio un error intentando abrir un archivo en almacenamiento local: " + e);
			}
		}
		
		// Es una URL
		InputStream tmpStream;
		try {
			if (waitDialog) {
				final ProgressMonitorInputStream pmis = new ProgressMonitorInputStream(
					c,
					"Leyendo " + uri.toURL().toString(),
					uri.toURL().openStream()
				);
				pm = pmis.getProgressMonitor(); 
//					pm.setMillisToDecideToPopup(0);
//					pm.setMillisToPopup(0);
				
				// Las URL pocas veces informan del tamano del fichero, asi que ponemos un valor alto
				// por defecto para segurarnos de que el dialogo se muestra
				pm.setMaximum(10000000);
				
				tmpStream = new BufferedInputStream(pmis);
			}
			else tmpStream = new BufferedInputStream(uri.toURL().openStream());
		}
		catch(final Throwable e) {
			if (pm != null) pm.close();
			throw new AOException(
				"Ocurrio un error intentando abrir la URI '" + uri.toASCIIString() + "' como URL: " + e
			);
		}
		// Las firmas via URL fallan en la descarga por temas de Sun, asi que descargamos primero
		// y devolvemos un Stream contra un array de bytes
		final byte[] tmpBuffer;
		try {
			tmpBuffer = getDataFromInputStream(tmpStream);
		}
		catch(final Throwable e) {
			if (pm != null) pm.close();
			throw new AOException("Error leyendo el fichero remoto '" + uri.toString() + "': " + e);
		}
		
		// Hay que cerrar el ProgressMonitor a mano:
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4174850
		if (pm != null) pm.close();

//		Logger.getLogger("es.gob.afirma").info(
//			"Leido fichero de " + tmpBuffer.length + " bytes:\n" + new String(tmpBuffer)
//		);
		return new java.io.ByteArrayInputStream(tmpBuffer);
	}
	
	/**
	 * Lee un flujo de datos de entrada y los recupera en forma de array de bytes. Este
	 * m&eacute;todo consume pero no cierra el flujo de datos de entrada.
	 * @param input Flujo de donde se toman los datos.
	 * @return Los datos obtenidos del flujo.
	 * @throws IOException Cuando ocurre un problema durante la lectura
	 */
	public final static byte[] getDataFromInputStream(final InputStream input) throws IOException {
		if (input == null) return new byte[0];
		int nBytes = 0;
		byte[] buffer = new byte[4096];
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while((nBytes = input.read(buffer)) != -1) {
			baos.write(buffer, 0, nBytes);
		}
		return baos.toByteArray();
	}

	/**
	 * Obtiene el directorio del perfil de usuario de Mozilla / Firefox.
	 * @return Ruta completa del directorio del perfil de usuario de Mozilla / Firefox
	 */
	public final static String getMozillaUserProfileDirectory() {
		return getMozillaUserProfileDirectory(System.getProperty("os.name").contains("indows"));
	}

	/**
	 * Obtiene el directorio del perfil de usuario de Mozilla / Firefox.
	 * @param windows <code>true</code> si de debe buscar el directorio en un entorno Windows,
	 *                <code>false</code> si el entorno es UNIX
	 * @return Ruta completa del directorio del perfil de usuario de Mozilla / Firefox
	 */
	public final static String getMozillaUserProfileDirectory(final boolean windows) {

		File regFile;
		if (windows) {
			final String appDataDir = WinRegistryWrapper.getString(
					WinRegistryWrapper.HKEY_CURRENT_USER,
				"Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders",
				"AppData"
			);
			if (appDataDir != null) {
				String finalDir = null;
				// En Firefox usamos preferentemente el profiles.ini
				regFile = new File(appDataDir + "\\Mozilla\\Firefox\\profiles.ini");
				try {
					if (regFile.exists())
						finalDir = NSPreferences.getFireFoxUserProfileDirectory(regFile);
				}
				catch (Throwable e) {
					Logger.getLogger("es.gob.afirma").severe(
							"Ocurrio un error obteniendo el directorio de perfil de usuario de Firefox, " +
							"se devolvera null: " + e
					);
					return null;
				}
				if (finalDir != null) {
					try {
						return AOWinNativeUtil.getShortPathName(finalDir).replace('\\', '/');
					}
					catch(final Throwable e) {
						Logger.getLogger("es.gob.afirma").warning(
							"No se ha podido obtener el nombre corto de la ruta de NSS en Firefox ('" +
							finalDir +
							"'), se usara el nombre largo: " +
							e
						);
						
						return finalDir.replace('\\', '/');
					}
				}
				// Hemos probado el de Firefox, vamos ahora con el de Mozilla
				regFile = new File(appDataDir + "\\Mozilla\\registry.dat");
				try {
					if (regFile.exists()) finalDir = NSPreferences.getNS6UserProfileDirectory(regFile);
				}
				catch (Throwable e) {
					Logger.getLogger("es.gob.afirma").severe(
						"Ocurrio un error obteniendo el directorio de perfil de usuario de Mozilla, " +
						"se devolvera null: " + e
					);
					return null;
				}
				if (finalDir != null) {
					try {
						return AOWinNativeUtil.getShortPathName(finalDir).replace('\\', '/');
					}
					catch(final Throwable e) {
						Logger.getLogger("es.gob.afirma").warning(
							"No se ha podido obtener el nombre corto de la ruta de NSS en Mozilla ('" +
							finalDir +
							"'), se usara el nombre largo: " +
							e
						);
						return finalDir.replace('\\', '/');
					}
				}
			}
			Logger.getLogger("es.gob.afirma").severe(
					"Ocurrio un error obteniendo el directorio de perfil de usuario de Mozilla/Firefox (Windows), " +
					"se devolvera null"
			);
			return null;
		}
		
		// No es Windows, entonces es UNIX

		// Probamos primero con "profiles.ini" de Firefox
		regFile = new File(System.getProperty("user.home") + "/.mozilla/firefox/profiles.ini");
		try {
			if (regFile.exists()) return NSPreferences.getFireFoxUserProfileDirectory(regFile);
		}
		catch (Exception e) {
			Logger.getLogger("es.gob.afirma").severe(
					"Ocurrio un error obteniendo el directorio de perfil de usuario de Firefox, " +
					"se devolvera null: " + e
			);
			return null;
		}

		// Si es un Mac OS X, profiles.ini esta en una ruta distinta...
		regFile = new File(System.getProperty("user.home") + "/Library/Application Support/Firefox/profiles.ini");
		try {
			if (regFile.exists()) return NSPreferences.getFireFoxUserProfileDirectory(regFile);
		}
		catch (Exception e) {
			Logger.getLogger("es.gob.afirma").severe(
					"Ocurrio un error obteniendo el directorio de perfil de usuario de Firefox (" +
					regFile.getAbsolutePath() + "), se devolvera null: " + e
			);
			return null;
		}

		// Y luego con el registro clasico de Mozilla
		regFile = new File(System.getProperty("user.home") + "/.mozilla/appreg");
		try {
			if (regFile.exists()) return NSPreferences.getNS6UserProfileDirectory(regFile);
		}
		catch (Exception e) {
			Logger.getLogger("es.gob.afirma").severe(
					"Ocurrio un error obteniendo el directorio de perfil de usuario de Firefox, " +
					"se devolvera null: " + e
			);
			return null;
		}

		Logger.getLogger("es.gob.afirma").severe(
				"Ocurrio un error obteniendo el directorio de perfil de usuario de Mozilla/Firefox (UNIX), " +
				"se devolvera null"
		);
		return null;
	}

	/** 
	 * Obtiene el directorio de instalaci&oacute;n del entorno de ejecuci&oacute;n de Java
	 * actualmente en uso.
	 * Copiado de com.sun.deploy.config.Config. 
	 * @return Directorio del entorno de ejecuci&oacute;n de Java
	 * @throws AOException Cuando ocurre cualquier problema durante el proceso
	 */
	public final static String getJavaHome() throws AOException {
		String ret = null;
		try {
			ret = System.getProperty("jnlpx.home");
		}
		catch(final Throwable e) {
			Logger.getLogger("es.gob.afirma").warning(
				"No se ha podido leer 'jnlp.home', se intentara 'java.home'"
			);
		}
		if (ret != null) {
			// use jnlpx.home if available
			// jnlpx.home always point to the location of the
			// jre bin directory (where javaws is installed)
			return ret.substring(0, ret.lastIndexOf(File.separator));
		}

		try {
			return System.getProperty("java.home");
		} 
		catch (Throwable e) {
			Logger.getLogger("es.gob.afirma").warning(
					"No se ha podido leer 'java.home'"
				);
		}
		
		throw new AOException("No ha podido recuperar el directorio de Java");
	}

	/**
	 * En sistemas Windows, a&ntilde;ade un directorio al PATH del sistema. Si el PATH en el
	 * registro de Windows es de tipo REG_EXPAND_SZ lo convierte a REG_SZ
	 * @param dir Directorio a a&ntilde;adir al PATH
	 * @throws AOException Cuando ocurre cualquier problema durante el proceso o si se invoca al
	 *                     m&eacute;todo en un sistema no Windows
	 */
	public final static void addDirToPath(final String dir) throws AOException {
		
		if (dir == null) {
			Logger.getLogger("No se puede anadir un directorio nulo al PATH, se ignorara la llamada");
			return;
		}

		if (!System.getProperty("os.name").contains("indows")) throw new AOException(
				"Solo se puede establecer el Path en sistemas Windows"
		);
		if ("".equals(dir)) throw new AOException(
				"El directorio a anadir al Path no puede ser ni nulo ni vacio"
		);
		if(dir.equals(System.getProperty("file.separator"))) throw new AOException(
				"El directorio a anadir al Path no puede ser unicamente el separador de ficheros"
		);
		
		// Retiramos la barra final en caso de haberla
		final String dirToAdd = dir.endsWith(System.getProperty("file.separator")) ? dir.substring(0, dir.length()-System.getProperty("file.separator").length()) : dir;
		
		// Si ya esta el directorio en el path, abortamos la operacion
		String[] paths = System.getProperty("java.library.path").split(System.getProperty("path.separator"));
		for(String path : paths) if(dirToAdd.equals(path) || (path.endsWith(System.getProperty("file.separator")) && dirToAdd.equals(path.substring(0, path.length()-System.getProperty("file.separator").length())))) return;
		
		// Cuidado!! El Path es un REG_EXPAND_SZ, que WinRegistry no soporta, asi que lo obtenemos con un
		// get() directamente y haciendo el casting a byte[]
		// Puede traer un caracter extrano al final de la linea, mejor limpiar con trim()
		// Leemos con un get y luego diferenciamos si es un REG_SZ o un REG_SZ_EXPAND

		// Intentaremos agregar los directorio al path de la "CurrentControlSet" (sesion de Windows abierta).
		// Si no existe trataremos de agregarla a las sesiones de la 1 a la 9 ("ControlSet001" - "ControlSet009").
		String controlSetName = "CurrentControlSet";
		

		Object out = WinRegistryWrapper.get(
				WinRegistryWrapper.HKEY_LOCAL_MACHINE,
				"SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment",
				"Path"
		);

		if(out == null) {
			int i = 0;
			do {
				i++;
				out = WinRegistryWrapper.get(
						WinRegistryWrapper.HKEY_LOCAL_MACHINE,
						"SYSTEM\\ControlSet00"+ i + "\\Control\\Session Manager\\Environment",
						"Path"
				);
			} while (out == null && i < 9);

			// Establecemos el ControlSet adecuado
			if(out != null) controlSetName = "ControlSet00" + i; 
		}

		String actualPath;
		if (out instanceof String) actualPath = out.toString();
		else if (out instanceof byte[]) actualPath = new String((byte[])out);
		else throw new AOException(
				"No se pudo establecer el Path, la lectura del Path actual devolvio un tipo " + out.getClass().toString()
		);

		// Expandimos el String con el caracter %SystemRoot%
		String systemRoot = getSystemRoot();

		if (systemRoot != null) {
			// Se puede encontrar en MizedCase y el LowerCase
			// Podr�a haber MixedCase que no siguiese el patron de 'S' y 'R' en mayusculas, pero no los
			// tratamos. Evitamos la tentacion de pasar el Path a LowerCase porque si ha instalado MS-SFU
			// Windows se convierte en Case Sensitive y podemos hacer un estropicio
			actualPath = actualPath.replace("%SystemRoot%", systemRoot).replace("%systemroot%", systemRoot);
		}

		// Comprobamos que el path que deseamos anadir no estuviese ya en el registro. Esto puede
		// suceder cuando no se ha recargado la variable path del sistema tomando el nuevo valor del
		// registro
		paths = actualPath.split(System.getProperty("path.separator"));
		for(String path : paths) if(dirToAdd.equals(path) || (path.endsWith(System.getProperty("file.separator")) && dirToAdd.equals(path.substring(0, path.length()-System.getProperty("file.separator").length())))) return;
		
		// Ahora debemos establecer el nuevo PATH con WinRegistry.setStringValue()
		// CUIDADO!!! Convierte los REG_SZ_EXPAND en REG_SZ
		if (!WinRegistryWrapper.setStringValue(
			WinRegistryWrapper.HKEY_LOCAL_MACHINE,
			"SYSTEM\\" + controlSetName + "\\Control\\Session Manager\\Environment",
			"Path",
			dirToAdd + File.pathSeparator + actualPath
		)) throw new AOException(
			"Ocurrio un error indefinido intentando anadir el directorio '" + dirToAdd +
			"' al Path del sistema (" + actualPath + "), el sistema puede haber quedado " +
			"inconsistente"
		);
	}

	/**
	 * Obtiene el directorio principal del sistema operativo del sistema.
	 * @return Directorio principal del sistema operativo
	 */
	public final static String getSystemRoot() {
		if (!System.getProperty("os.name").contains("indows")) return File.separator;
		String systemRoot = null;
		final String defaultSystemRoot = "C:\\WINDOWS";
		try {
			systemRoot = WinRegistryWrapper.getString(
				WinRegistryWrapper.HKEY_LOCAL_MACHINE, 
				"SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", 
				"SystemRoot"
			);
		}
		catch(final Throwable e) {
			Logger.getLogger("es.gob.afirma").severe(
				"No se ha podido obtener el directorio principal de Windows accediendo al registro, " + 
				"se probara con 'C:\\WINDOWS': " + e
			);
		}
		if (systemRoot == null) {
			final File winSys32 = new File(defaultSystemRoot + "\\SYSTEM32");
			if (winSys32.exists() && winSys32.isDirectory()) return defaultSystemRoot;
		}
		if (systemRoot == null) {
			Logger.getLogger("es.gob.afirma").warning("No se ha encontrado el directorio ra&iacute;z del sistema, se devolver&aacute;: "+File.separator);
			systemRoot = File.separator; 
		}
		return systemRoot;
	}
	
	/**
	 * Devuelve el directorio principal de bibliotecas del sistema.
	 * Importante: No funciona correctamente en Windows de 64 bits //FIXME
	 * @return Directorio principal de bibliotecas
	 */
	public final static String getSystemLibDir() {
		if (System.getProperty("os.name").contains("indows")) {
			String systemRoot = AOUtil.getSystemRoot();
			if (systemRoot == null) {
				Logger.getLogger("es.gob.afirma").warning(
					"No se ha podido determinar el directorio de Windows accediendo al registro, " +
					"se establecera 'C:\\WINDOWS\\' por defecto"
				);
				systemRoot = "c:\\windows\\"; 
			}
			if (!systemRoot.endsWith("\\")) systemRoot += "\\";
			return systemRoot + "System32";
		}
		return "/usr/lib";
	}
	
	/**
	 * Obtiene el nombre com&uacute;n (Common Name, CN) del titular de un certificado X.509.
	 * @param c Certificado X.509 del cual queremos obtener el nombre com&uacute;n
	 * @return Nombre com&uacute;n (Common Name, CN) del titular de un certificado X.509
	 */
	public final static String getCN(final X509Certificate c) {
		if (c==null) return null;
		return getCN(c.getSubjectDN().toString());
	}

	/**
	 * Obtiene el nombre com&uacute;n (Common Name, CN) de un <i>Principal</i> X.400.
	 * @param principal <i>Principal</i> del cual queremos obtener el nombre com&uacute;n
	 * @return Nombre com&uacute;n (Common Name, CN) de un <i>Principal</i> X.400
	 */
	public final static String getCN(final String principal) {
		if (principal==null) return null;
		List<Rdn> rdns;
		try {
			rdns = new LdapName(principal).getRdns();
		}
		catch(final Throwable e) {
			Logger.getLogger("es.gob.afirma").warning(
					"No se ha podido obtener el Common Name, se devolvera la cadena de entrada: " + e
			);
			return principal;
		}
		if (rdns != null && (!rdns.isEmpty())) {
			String ou = null;
			for (int j=0; j<rdns.size(); j++) {
				if (rdns.get(j).toString().startsWith("cn=") || rdns.get(j).toString().startsWith("CN=")) {
					return rdns.get(j).toString().substring(3);
				}
				else if(rdns.get(j).toString().startsWith("ou=") || rdns.get(j).toString().startsWith("OU=")) {
					ou = rdns.get(j).toString().substring(3);
				}
			}
			
			// En caso de no haber encontrado el Common Name y si la Organizational Unit,
			// devolvemos esta ultima
			if(ou != null) {
				return ou;
			}
			
			Logger.getLogger("es.gob.afirma").warning(
					"No se ha podido obtener el Common Name ni la Organizational Unit, se devolvera el fragmento mas significativo"
				);
			return rdns.get(rdns.size()-1).toString().substring(3);
		}
		Logger.getLogger("es.gob.afirma").warning("Principal no valido, se devolvera la entrada");
		return principal;
	}

	/**
	 * Comprueba si el uso un certificado concuerda con un filtro dado. 
	 * @param cert Certificado X.509 que queremos comprobar
	 * @param filter Filtro con los bits de uso (<i>KeyUsage</i>) a verificar
	 * @return <code>true</code> si el certificado concuerda con el filtro, <code>false</code>
	 *         en caso contrario
	 */
	public final static boolean matchesKeyUsageFilter(final X509Certificate cert, final Boolean[] filter) {
	    if (filter == null) return true;
		if (cert == null) return false;
		if (filter.length == 9) {
			boolean[] certUsage = cert.getKeyUsage();
			if (certUsage != null) {
				for (int j=0;j<certUsage.length;j++) {
					if (filter[j] != null && filter[j].booleanValue() != certUsage[j]) return false;
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Busca un fichero (o una serie de ficheros) en el PATH del sistema.
	 * Deja de buscar en la primera ocurrencia
	 * @param files Ficheros a buscar en el PATH
	 * @param excludeAFirma Excluye el directorio de instalaci&oacute;n de AFirma de la b&uacute;squeda
	 * @return Ruta completa del fichero encontrado en el PATH o <code>null</code> si no se encontr&oacute; nada 
	 */
	public final static String searchPathForFile(final String[] files, final boolean excludeAFirma) {
		if (files == null || files.length < 1) return null;
		
        // Si existe el primero con el PATH completo lo devolvemos sin mas
        if (new File(files[0]).exists()) return files[0];
        
		final StringTokenizer st = new StringTokenizer(
			System.getProperty("java.library.path"),
			File.pathSeparator
		);
		String libPath;
		while (st.hasMoreTokens()) {
			libPath = st.nextToken();
			if (libPath.startsWith(AOInstallParameters.getHomeApplication()) && excludeAFirma) continue;
			if (!libPath.endsWith(File.separator)) libPath = libPath + File.separator;
			File tmpFile;
			for (String f : files) {
				tmpFile = new File(libPath + f); 
				if (tmpFile.exists() && (!tmpFile.isDirectory())) return libPath + f; 
			}
	     }
		return null;
	}

	 /**
	  * Obtiene un n&uacute;mero de 16 bits a partir de dos posiciones de un array de octetos.
	  * @param src Array de octetos origen
	  * @param offset Desplazamiento desde el origen para el comienzo del par de octetos
	  * @return N&ueacute;mero entero de 16 bits (sin signo)
	  */
	 public final static int getShort(final byte[] src, final int offset) {
		 return (((src)[offset + 0] << 8) | (src)[offset + 1]);
	 }
	
	/**
	 * Caracterres aceptados en una codificaci&oacute;n Base64 seg&uacute;n la <a href="http://www.faqs.org/rfcs/rfc3548.html">RFC 3548</a>. 
	 * Importante: A&ntilde;adimos el car&aacute;cter &tilde; porque en ciertas codificaciones de Base64 est&aacute;
	 * aceptado, aunque no es nada recomendable
	 */
	private static final String base64Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz=_-\n+/0123456789\r~";
	
	/**
	 * @param data Datos a comprobar si podr6iacute;an o no ser Base64
	 * @return <code>true</code> si los datos proporcionado pueden ser una codificaci&oacute;n base64
	 *         de un original binario (que no tiene necesariamente porqu&eacute; serlo), <code>false</code>
	 *         en caso contrario
	 */
	public final static boolean isBase64(final byte[] data) {
	    
		//if (!new String(data).endsWith("=")) return false;
	    
	    // Comprobamos que la cadena tenga una longitud multiplo de 4 caracteres
	    String b64String = new String(data).trim();
	    if(b64String.replace("\r\n", "").replace("\n", "").length()%4 != 0) {
	        return false;
	    }
	    
	    // Comprobamos que todos los caracteres de la cadena pertenezcan al alfabeto base 64 
		for (byte b : data) if (!base64Alphabet.contains(new String(new byte[] {b}))) return false;
		return true;
	}
	
	/**
	  * Equivalencias de hexadecimal a texto por la posici&oacute;n del vector. Para
	  * ser usado en <code>hexify()</code>
	  */
	 private static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6',
	   '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	
	 /**
	  * Convierte un vector de octetos en una cadena de caracteres que contiene la
	  * representaci&oacute;n hexadecimal. Copiado directamente de
	  * opencard.core.util.HexString
	  * 
	  * @param abyte0
	  *         Vector de octetos que deseamos representar textualmente
	  * @param separator
	  *         Indica si han o no de separarse los octetos con un gui&oacute;n y en
	  *         l&iacute;neas de 16
	  * @return Representaci&oacute;n textual del vector de octetos de entrada
	  */
	 public final static String hexify(byte abyte0[], boolean separator) {
	      if (abyte0 == null) return "null";
	      
	      final StringBuffer stringbuffer = new StringBuffer(256);
	      int i = 0;
	      for (int j = 0; j < abyte0.length; j++) {
	       if (separator && i > 0) stringbuffer.append('-');
	       stringbuffer.append(HEX_CHARS[abyte0[j] >> 4 & 0xf]);
	       stringbuffer.append(HEX_CHARS[abyte0[j] & 0xf]);
	       ++i;
	       if (i == 16) { 
	        if (separator && j < abyte0.length-1) stringbuffer.append('\n');
	        i = 0;
	       }
	      }
	      return stringbuffer.toString();
	 }

	 /**
     * Convierte un vector de octetos en una cadena de caracteres que contiene la
     * representaci&oacute;n hexadecimal. Copiado directamente de
     * opencard.core.util.HexString
     * 
     * @param abyte0
     *         Vector de octetos que deseamos representar textualmente
     * @param separator
     *         Indica si han o no de separarse los octetos con un gui&oacute;n y en
     *         l&iacute;neas de 16
     * @return Representaci&oacute;n textual del vector de octetos de entrada
     */
    public static final String hexify(final byte abyte0[], final String separator) {
     if (abyte0 == null) return "null";
     
     final StringBuffer stringbuffer = new StringBuffer(256);
     for (int j = 0; j < abyte0.length; j++) {
      if (separator != null && j > 0) stringbuffer.append(separator);
      stringbuffer.append(HEX_CHARS[abyte0[j] >> 4 & 0xf]);
      stringbuffer.append(HEX_CHARS[abyte0[j] & 0xf]);
     }
     return stringbuffer.toString();
    }

	 /**
	  * Obtiene la ruta absoluta del fichero de almac&eacute;n de las claves de cifrado.
	  * @return Ruta absoluta del fichero.
	  */
	public final static String getCipherKeystore() {
		return AOInstallParameters.getUserHome() + "ciphkeys.jceks";
	}
	
	/**
	 * Obtiene el path del fichero de propiedades de configuraci&oacute;n del directorio LDAP.
	 * @return Path del fichero.
	 */
	public static String getLdapProperties() {
		return AOInstallParameters.getHomeApplication() + File.separator + "ldap.properties.xml";
	}
	
	/**
	 * Obtiene el path del fichero de propiedades de configuraci&oacute;n de pol&iacute;ticas de firma.
	 * @return Path del fichero.
	 */
	public static String getPoliciesProperties() {
		return AOInstallParameters.getHomeApplication() + File.separator + "policy.properties.xml";
	}
	
	/**
	 * Recupera un algoritmo de hash a partir del algoritmo de firma introducido s&oacute;lo para su
	 * uso para la generaci&oacute;n de hashes. Si el algoritmo no respeta el formato
	 * "ALGORITHM_HASH"+with+"AGORITHM_SIGN" se devuelve nulo. 
	 * @param signatureAlgorithm Nombre del algoritmo de firma
	 * @return Algoritmo de hash.
	 */
	public final static String getDigestAlgorithm(String signatureAlgorithm) {
		
		final int withPos = signatureAlgorithm.indexOf("with");
		if(withPos == -1) return null;
		
	    String digestAlg = signatureAlgorithm.substring(0, withPos);
        if(digestAlg.startsWith("SHA")) {
            digestAlg = "SHA-" + digestAlg.substring(3);
        }
	    return digestAlg;
	}
	
	/**
     * Recupera el texto con un identificador de versi&oacute;n a partir de un properties indicado
     * a trav&eacute;s de un <code>InputStream</code>. Las propiedades del properties que definen la
     * versi&oacute;n son:<br/>
     * <code><ul><li>version.mayor: Versi&oacute;n.</li>
     * <li>version.minor: Versi&oacute;n menor.</li>
     * <li>version.build: Contrucci&oacute;n</li>
     * <li>version.description: Descripci&oacute;n</li></ul></code>
     * El formato en el que se devuelve la versi&oacute;n ser&aacute; siempre:<br/>
     * <code>X.Y.Z Descripci&oacute;n</code><br/>
     * En donde <code>X</code>, <code>Y</code> y <code>Z</code> son la versi&oacute;n, subversi&oacute;n
     * y contrucci&oacute;n del cliente y puede tener uno o m&aacute;s d&iacute;gitos; y 
     * <code>Descripci&oacute;n</code> es un texto libre opcional que puede completar la
     * identificaci&oacute;n de la versi&oacute;n del cliente.</br>
     * Si no se indica alg&uacute;n de los n&uacute;meros de versi&oacute;n se indicar&aacute; cero ('0')
     * y si no se indica la descripci&oacute;n no se mostrar&aacute; nada.
     *  
     * @param is Datos del properties con la versi&oacute;n.
     * @return Identificador de la versi&oacute;n.
     */
    public final static String getVersion(final InputStream is) {
        final Properties p = new Properties();
        try {
            p.load(is);
        } 
        catch (final Throwable e) {
            Logger.getLogger("es.gob.afirma").warning("No se han podido obtener los datos de version del cliente de firma");
        }
        final StringBuilder version = new StringBuilder();
        version.append(p.getProperty("version.mayor", "0")).append(".")
            .append(p.getProperty("version.minor", "0")).append(".")
            .append(p.getProperty("version.build", "0"));
        
        final String desc = p.getProperty("version.description");
        if(desc != null && !desc.trim().equals("")) {
            version.append(" ").append(desc);
        }
        return version.toString();
        
    }
            
    /**
     * Genera una cadena representativa del &aacute;rbol que recibe.
     * @param tree &Aacute;rbol que se desea representar.
     * @param linePrefx Prefijo de cada l&iacute;nea de firma (por defecto, cadena vac&iacute;a).
     * @param identationString Cadena para la identaci&oacute;n de los nodos de firma (por defecto, tabulador).
     * @return Cadena de texto.
     */
    public final static String showTreeAsString(final TreeModel tree, String linePrefx, String identationString) {

        if(tree == null || tree.getRoot() == null) {
            Logger.getLogger("es.gob.afirma").severe("Se ha proporcionado un arbol de firmas vacio"); //$NON-NLS-1$
            return null;
        }

        if(!(tree.getRoot() instanceof DefaultMutableTreeNode)) {
            Logger.getLogger("es.gob.afirma").severe("La raiz del arbol de firmas no es de tipo DafaultMutableTreeNode"); //$NON-NLS-1$
            return null;
        }

        if(linePrefx == null) linePrefx = "";
        if(identationString == null) identationString = "\t";

        StringBuilder buffer = new StringBuilder();
        
        // Transformamos en cadenas de texto cada rama que surja del nodo raiz del arbol
        TreeNode root = (DefaultMutableTreeNode)tree.getRoot();
        for(int i=0; i < root.getChildCount(); i++) {
            archiveTreeNode(root.getChildAt(i), 0, linePrefx, identationString, buffer); 
        }

        return buffer.toString();
    }

    /**
     * Transforma en cadena una rama completa de un &aacute;rbol. Para una correcta indexaci&oacute; es necesario
     * indicar la profundidad en la que esta el nodo del que pende la rama. En el caso de las ramas que penden del
     * nodo ra&iacute;z del &aacute;rbol la profundidad es cero (0).
     * @param node Nodo del que cuelga la rama.
     * @param depth Profundidad del nodo del que pende la rama.
     * @param linePrefx Prefijo de cada l&iacute;nea de firma (por defecto, cadena vac&iacute;a).
     * @param identationString Cadena para la identaci&oacute;n de los nodos de firma (por defecto, tabulador).
     * @param buffer Buffer en donde se genera la cadena de texto.
     */
    private final static void archiveTreeNode(final TreeNode node, 
                                              final int depth,
                                              final String linePrefx,
                                              final String identationString,
                                              final StringBuilder buffer) {
        buffer.append('\n').append(linePrefx);
        for(int i=0; i<depth; i++) buffer.append(identationString);
        buffer.append(((DefaultMutableTreeNode)node).getUserObject());
        for(int i=0; i<node.getChildCount(); i++) {
            archiveTreeNode(node.getChildAt(i), depth+1, linePrefx, identationString, buffer);
        }
    }

    /**
     * Carga una librer&iacute;a nativa del sistema. 
     * @param path Ruta a la libreria de sistema.
     */
    public static void loadNativeLibrary(String path) {
        
    	boolean copyOK = false;
        int pos = path.lastIndexOf('.');
        File file = new File(path);
        File tempLibrary = null;
        try {
            tempLibrary = File.createTempFile(
            		pos < 1 ? file.getName() : file.getName().substring(0, file.getName().indexOf('.')),
                    pos < 1 || pos == path.length()-1 ? null : path.substring(pos));
            
            // Copiamos el fichero
            copyOK = copyFile(file, tempLibrary);
        } catch (Exception e) {
            Logger.getLogger("es.gob.afirma").warning("Ocurrio un error al generar una nueva instancia de la libreria "
                    + path + " para su carga: "+e);
        }
        
        // Pedimos borrar los temporales cuando se cierre la JVM
        if(tempLibrary != null) {
        	try {
        		tempLibrary.deleteOnExit();
        	} catch (Exception e) { }
        }
        
        Logger.getLogger("es.gob.afirma").info("Cargamos "+(tempLibrary == null ? path : tempLibrary.getAbsolutePath()));
        System.load((copyOK && tempLibrary != null) ? tempLibrary.getAbsolutePath() : path);
    }
    
    /**
     * Copia un fichero.
     * @param source Fichero origen con el contenido que queremos copiar.
     * @param dest Fichero destino de los datos.
     * @return Devuelve <code>true</code> si la operac&oacute;n  finaliza correctamente,
     * <code>false</code> en caso contrario.
     */
    public static boolean copyFile(File source, File dest) {
    	if (source == null || dest == null) return false;
    	try {
    	 FileChannel in = new FileInputStream(source).getChannel();
    	 FileChannel out = new FileOutputStream(dest).getChannel();
         MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, in.size());
         out.write(buf);
         
         // Cerramos los canales sin preocuparnos de que lo haga correctamente 
         try { in.close(); } catch (Exception e) {}
         try { out.close(); } catch (Exception e) {}
    	}
    	catch(Throwable e) {
    		Logger.getLogger("es.gob.afirma").severe(
				"No se ha podido copiar el fichero origen '" +
				source.getName() +
				"' al destino '" +
				dest.getName() +
				"': " +
				e
			);
    		return false;
    	}
    	return true;
    }
    
}
