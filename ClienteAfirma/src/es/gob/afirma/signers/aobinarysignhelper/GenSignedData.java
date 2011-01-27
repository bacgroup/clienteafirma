/*
 * Este fichero forma parte del Cliente @firma. 
 * El Cliente @firma es un applet de libre distribuci�n cuyo c�digo fuente puede ser consultado
 * y descargado desde www.ctt.map.es.
 * Copyright 2009,2010 Gobierno de Espa�a
 * Este fichero se distribuye bajo las licencias EUPL versi�n 1.1  y GPL versi�n 3, o superiores, seg�n las
 * condiciones que figuran en el fichero 'LICENSE.txt' que se acompa�a.  Si se   distribuyera este 
 * fichero individualmente, deben incluirse aqu� las condiciones expresadas all�.
 */


package es.gob.afirma.signers.aobinarysignhelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.BERConstructedOctetString;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERUTCTime;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.asn1.cms.SignerIdentifier;
import org.bouncycastle.asn1.cms.SignerInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.TBSCertificateStructure;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.ietf.jgss.Oid;

import sun.security.x509.AlgorithmId;
import es.gob.afirma.exceptions.AOException;
import es.gob.afirma.misc.AOCryptoUtil;

/**
 *
 * Clase que implementa firma digital PKCS#7/CMS SignedData.
 * La Estructura del mensaje es la siguiente:<br>
 * <pre><code>
 * SignedData ::= SEQUENCE {
 *                     version           Version,
 *                     digestAlgorithms  DigestAlgorithmIdentifiers,
 *                     contentInfo       ContentInfo,
 *                     certificates      [0]  CertificateSet OPTIONAL,
 *                     crls              [1]  CertificateRevocationLists OPTIONAL,
 *                    signerInfos       SignerInfos
 *                  }
 *
 * Donde signerInfo:
 *
 * SignerInfo ::= SEQUENCE {
 *                     version                    Version,
 *                     signerIdentifier           SignerIdentifier,
 *                     digestAlgorithm            DigestAlgorithmIdentifier,
 *                     authenticatedAttributes    [0]  Attributes OPTIONAL,
 *                     digestEncryptionAlgorithm  DigestEncryptionAlgorithmIdentifier,
 *                     encryptedDigest            EncryptedDigest,
 *                     unauthenticatedAttributes  [1]  Attributes OPTIONAL
 *                   }
 *</code></pre>
 * La implementaci&oacute;n del c&oacute;digo ha seguido los pasos necesarios para crear un
 * mensaje SignedData de BouncyCastle: <a href="http://www.bouncycastle.org/">www.bouncycastle.org</a>
 */
public class GenSignedData extends SigUtils {

    ASN1Set signedAttr2;
    /**
     * M&eacute;odo que genera una firma digital usando el sitema conocido como
     * SignedData y que podr&aacute; ser con el contenido del fichero codificado
     * o s&oacute;lo como referencia del fichero.
     *
     * @param parameters    Par&aacute;metros necesarios para obtener los datos
     *                      de SignedData.
     *
     * @param omitContent   Par&aacute;metro que indica si en la firma va el
     *                      contenido del fichero o s&oacute;lo va de forma
     *                      referenciada.
     *
     * @param applyTimestamp Si se aplica el Timestamp o no.
     *
     * @param dataType      Identifica el tipo del contenido a firmar.
     *
     * @param keyEntry      Clave privada del firmante.
     * 
	 * @param atrib			Atributos firmados opcionales. 
	 * 
	 * @param uatrib		Atributos no autenticados firmados opcionales.
     * 
     * @param messageDigest	Hash a aplicar en la firma.
     *
     * @return  La firma generada codificada.
     *
     * @throws java.security.NoSuchAlgorithmException Si no se soporta alguno de los algoritmos de firma o huella digital
     * @throws java.security.cert.CertificateException Si se produce alguna excepci&oacute;n con los certificados de firma.
     * @throws java.io.IOException Cuando ocurre un error durante el proceso de descifrado (formato o clave incorrecto,...)
     * @throws AOException Cuando ocurre un error durante el proceso de descifrado (formato o clave incorrecto,...)
     */
    public byte[] generateSignedData(P7ContentSignerParameters parameters,
            boolean omitContent,
            boolean applyTimestamp,
            Oid dataType,
            PrivateKeyEntry keyEntry,
            HashMap<Oid, byte[]> atrib,
            HashMap<Oid, byte[]> uatrib,
            byte[] messageDigest)
            throws NoSuchAlgorithmException, CertificateException, IOException, AOException {

        if (parameters == null) throw new NullPointerException(
        	"Los parametros no pueden ser nulos"
        );

        // 1. VERSION
        // la version se mete en el constructor del signedData y es 1

        // 2. DIGESTALGORITM
        // buscamos que timo de algoritmo es y lo codificamos con su OID

        ASN1EncodableVector digestAlgs = new ASN1EncodableVector();
        AlgorithmIdentifier digAlgId;

        String signatureAlgorithm = parameters.getSignatureAlgorithm();
        String digestAlgorithm = null;
        String keyAlgorithm = null;
        int with = signatureAlgorithm.indexOf("with");
        if (with > 0) {
            digestAlgorithm = AOCryptoUtil.getDigestAlgorithmName(signatureAlgorithm);
            int and = signatureAlgorithm.indexOf("and", with + 4);
            if (and > 0) keyAlgorithm = signatureAlgorithm.substring(with + 4, and);
            else keyAlgorithm = signatureAlgorithm.substring(with + 4);
        }

        AlgorithmId digestAlgorithmId = AlgorithmId.get(digestAlgorithm);
        try {
            digAlgId = makeAlgId(digestAlgorithmId.getOID().toString(), digestAlgorithmId.getEncodedParams());
        }
        catch (Throwable e) {
        	e.printStackTrace();
            throw new IOException("Error de codificacion: " + e);
        }

        digestAlgs.add(digAlgId);

        // 3. CONTENTINFO
        // si se introduce el contenido o no

        ContentInfo encInfo = null;
        DERObjectIdentifier contentTypeOID = new DERObjectIdentifier(dataType.toString());

        if (omitContent == false) {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            byte[] content2 = parameters.getContent();
            CMSProcessable msg = new CMSProcessableByteArray(content2);
            try {
            	msg.write(bOut);
            }
            catch (Throwable ex) {
                throw new IOException("Error en la escritura del procesable CMS: " + ex);
            }
            encInfo = new ContentInfo(contentTypeOID, new BERConstructedOctetString(bOut.toByteArray()));
        }
        else encInfo = new ContentInfo(contentTypeOID, null);

        // 4.    CERTIFICADOS
        // obtenemos la lista de certificados

        ASN1Set certificates = null;
        X509Certificate[] signerCertificateChain = parameters.getSignerCertificateChain();

        if (signerCertificateChain.length != 0) {
            // descomentar lo de abajo para version del rfc 3852
            List<DEREncodable> ce = new ArrayList<DEREncodable>();
            for (int i=0; i<signerCertificateChain.length;i++)
                ce.add(X509CertificateStructure.getInstance(ASN1Object.fromByteArray(signerCertificateChain[i].getEncoded())));
            certificates = createBerSetFromList(ce);

            //y comentar esta parte de abajo
//            ASN1EncodableVector v = new ASN1EncodableVector();
//            v.add(X509CertificateStructure.getInstance(ASN1Object.fromByteArray(signerCertificateChain[0].getEncoded())));
//            certificates = new BERSet(v);
            
        }


        ASN1Set certrevlist = null;

        // 5. SIGNERINFO
        // raiz de la secuencia de SignerInfo
        ASN1EncodableVector signerInfos = new ASN1EncodableVector();

        TBSCertificateStructure tbs = TBSCertificateStructure.getInstance(ASN1Object.fromByteArray(signerCertificateChain[0].getTBSCertificate()));
        IssuerAndSerialNumber encSid = new IssuerAndSerialNumber(tbs.getIssuer(), tbs.getSerialNumber().getValue());

        SignerIdentifier identifier = new SignerIdentifier(encSid);

        //AlgorithmIdentifier
        digAlgId = new AlgorithmIdentifier(new DERObjectIdentifier(digestAlgorithmId.getOID().toString()), new DERNull());

        //// ATRIBUTOS

        //ATRIBUTOS FIRMADOS
        ASN1Set signedAttr = null;        
        signedAttr = generateSignerInfo(signerCertificateChain[0], digestAlgorithm, parameters.getContent(),dataType, applyTimestamp,atrib, messageDigest);
        
        //ATRIBUTOS NO FIRMADOS.

        ASN1Set unSignedAttr = null;
        unSignedAttr = generateUnsignerInfo(uatrib);


        ////  FIN ATRIBUTOS

        //digEncryptionAlgorithm
        AlgorithmId digestAlgorithmIdEnc = AlgorithmId.get(keyAlgorithm);
        AlgorithmIdentifier encAlgId;
        try {
            encAlgId = makeAlgId(digestAlgorithmIdEnc.getOID().toString(), digestAlgorithmIdEnc.getEncodedParams());
        }
        catch (Throwable e) {
            throw new IOException("Error de codificacion: " + e);
        }

        ASN1OctetString sign2 = firma(signatureAlgorithm, keyEntry);
        signerInfos.add(
    		new SignerInfo(
    	        	identifier,
    	        	digAlgId,
    	        	signedAttr,
    	        	encAlgId,
    	        	sign2,
    	        	unSignedAttr//null //unsignedAttr
	        )
        );

        // construimos el Signed Data y lo devolvemos
        return new ContentInfo(
        	PKCSObjectIdentifiers.signedData,
        	new SignedData(
                new DERSet(digestAlgs),
                encInfo,
                certificates,
                certrevlist,
                new DERSet(signerInfos)
            )
        ).getDEREncoded();

    }

    /**
     *  M&eacute;todo que genera la parte que contiene la informaci&oacute;n del Usuario.
     *  Se generan los atributos que se necesitan para generar la firma.
     *
     * @param cert              Certificado necesario para la firma.
     * @param digestAlgorithm   Algoritmo Firmado.
     * @param datos             Datos firmados.
     * @param datatype          Identifica el tipo del contenido a firmar.
     * @param timestamp			Introducir TimeStaming
     * @param atrib             Lista de atributos firmados que se insertar&aacute;n dentro del archivo de firma.
     *
     * @return      Los atributos firmados de la firma.
     *
     * @throws java.security.NoSuchAlgorithmException
     */
    private ASN1Set generateSignerInfo(X509Certificate cert,
                            String digestAlgorithm,
                            byte[] datos,
                            Oid datatype,
                            boolean timestamp,
                            HashMap<Oid, byte[]> atrib,
                            byte[] messageDigest)
                        throws NoSuchAlgorithmException {

        //// ATRIBUTOS

        //authenticatedAttributes
        ASN1EncodableVector ContexExpecific = new ASN1EncodableVector();

        //tipo de contenido
        ContexExpecific.add(new Attribute(CMSAttributes.contentType, new DERSet(new DERObjectIdentifier(datatype.toString()))));

        //fecha de firma
        if (timestamp){
        ContexExpecific.add(new Attribute(CMSAttributes.signingTime, new DERSet(new DERUTCTime(new Date()))));
        }
        
        // Los DigestAlgorithms con SHA-2 tienen un guion:
        if (digestAlgorithm.equals("SHA512")) digestAlgorithm = "SHA-512";
        else if (digestAlgorithm.equals("SHA384")) digestAlgorithm = "SHA-384";
        else if (digestAlgorithm.equals("SHA256")) digestAlgorithm = "SHA-256";
        
        // Si nos viene el hash de fuera no lo calculamos
        final byte[] md;
        if (messageDigest == null || messageDigest.length < 1) {
    		md = MessageDigest.getInstance(digestAlgorithm).digest(datos);
    	}
        else md = messageDigest;
        
        //MessageDigest
        ContexExpecific.add(
            new Attribute(
            	CMSAttributes.messageDigest,
                new DERSet(new DEROctetString(md.clone()))
            )
        );

        //Serial Number
        // comentar lo de abajo para version del rfc 3852
        ContexExpecific.add(
    		new Attribute(
				X509Name.SERIALNUMBER,
                new DERSet(new DERPrintableString(cert.getSerialNumber().toString()))
			)
        );

        //agregamos la lista de atributos a mayores.
        if (atrib.size()!=0){
        	
        	Iterator<Map.Entry<Oid, byte[]>> it = atrib.entrySet().iterator();
        	while (it.hasNext()) {
        	    Map.Entry<Oid, byte[]> e = it.next();
        	ContexExpecific.add(
                    new Attribute(
                        // el oid
                        new DERObjectIdentifier((e.getKey()).toString()),
                        // el array de bytes en formato string
                        new DERSet(new DERPrintableString(e.getValue()))
                    )
                );
        	}
        	
        }

        signedAttr2 = getAttributeSet(new AttributeTable(ContexExpecific));
        
        return getAttributeSet(new AttributeTable(ContexExpecific));

    }

    /**
     *  M&eacute;todo que genera la parte que contiene la informaci&oacute;n del Usuario.
     *  Se generan los atributos no firmados.
     *
     * @param uatrib    Lista de atributos no firmados que se insertar&aacute;n dentro del archivo de firma.
     *
     * @return      Los atributos no firmados de la firma.
     */
    private ASN1Set generateUnsignerInfo(HashMap<Oid, byte[]> uatrib){

        //// ATRIBUTOS

        //authenticatedAttributes
        ASN1EncodableVector ContexExpecific = new ASN1EncodableVector();


        //agregamos la lista de atributos a mayores.
        if (uatrib.size()!=0){
        	Iterator<Map.Entry<Oid, byte[]>> it = uatrib.entrySet().iterator();
        	while (it.hasNext()) {
        	    Map.Entry<Oid, byte[]> e = it.next();
        	ContexExpecific.add(
                    new Attribute(
                        // el oid
                        new DERObjectIdentifier((e.getKey()).toString()),
                        // el array de bytes en formato string
                        new DERSet(new DERPrintableString(e.getValue()))
                    )
                );
        	}
        }
        else{
            return null;
        }

        return getAttributeSet(new AttributeTable(ContexExpecific));

    }

    /**
     * Realiza la firma usando los atributos del firmante.
     * @param signatureAlgorithm    Algoritmo para la firma
     * @param keyEntry              Clave para firmar.
     * @return                      Firma de los atributos.
     * @throws es.map.es.map.afirma.exceptions.AOException
     */
    private ASN1OctetString firma (String signatureAlgorithm, PrivateKeyEntry keyEntry) throws AOException{
        
    	Signature sig = null;
		try {
			sig = Signature.getInstance(signatureAlgorithm);
		} 
		catch (Throwable e) {
            //e.printStackTrace();
            throw new AOException("No ha instalado soporte para el algoritmo de firma '" + signatureAlgorithm + "': " + e);
		}

        //Indicar clave privada para la firma
		try {
			sig.initSign(keyEntry.getPrivateKey());
		} catch (Throwable e) {
		    Logger.getLogger("es.gob.afirma").severe("Error inicializando la firma, clave de tipo: "+keyEntry.getPrivateKey().getClass().getName());
			throw new AOException(
				"Error al obtener la clave de firma para el algoritmo '" + signatureAlgorithm + "': " + e
			);
		}
		
        // Actualizamos la configuracion de firma
		try {
			sig.update(signedAttr2.getEncoded(ASN1Encodable.DER));
		}
		catch (Throwable e) {
			throw new AOException(
				"Error al configurar la informacion de firma o al obtener los atributos a firmar: " + e
			);
		}

        
        //firmamos.
        byte[] realSig=null; 
        try {
			realSig = sig.sign();
		} 
        catch (Throwable e) {
        	e.printStackTrace();
			throw new AOException("Error durante el proceso de firma: " + e);
		}

        ASN1OctetString encDigest = new DEROctetString(realSig);

        return encDigest;

    }


}
