/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 11/01/11
 * You may contact the copyright holder at: soporte.afirma5@mpt.es
 */

package es.gob.afirma.keystores.main.common;


/** Indica que ocurri&oacute; un error intentando obtener un <code>KeyStore</code>, pero que se obtuvo uno de un tipo alternativo.
 * @version 0.1 */
public final class AOKeystoreAlternativeException extends Exception {

    private static final long serialVersionUID = -1536411480952188376L;

    private final AOKeyStore alternativeKs;

    /** Crea la excepci&oacute;n con un mensaje determinado y un almac&eacute;n
     * alternativo que podr&iacute;a usarse como sustituto.
     * @param ks
     *        Almac&eacute;n de claves y certificados alternativo
     * @param desc
     *        Mensaje descriptivo de la excepci&oacute;n.
     * @param e
     *        Excepci&oacute;n que ha causado el lanzamiento de esta. */
    AOKeystoreAlternativeException(final AOKeyStore ks, final String desc, final Exception e) {
        super(desc, e);
        this.alternativeKs = ks;
    }

    /** Crea la excepci&oacute;n con un mensaje determinado y un almac&eacute;n
     * alternativo que podr&iacute;a usarse como sustituto.
     * @param ks
     *        Almac&eacute;n de claves y certificados alternativo
     * @param desc
     *        Mensaje descriptivo de la excepci&oacute;n. */
    AOKeystoreAlternativeException(final AOKeyStore ks, final String desc) {
        super(desc);
        this.alternativeKs = ks;
    }

    /** Obtiene el almac&eacute;n alternativo que podr&iacute;a usarse como
     * sustituto.
     * @return Almac&eacute;n de claves y certificados alternativo */
    public AOKeyStore getAlternativeKsm() {
        return this.alternativeKs;
    }

}
