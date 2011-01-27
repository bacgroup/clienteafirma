/*
 * Este fichero forma parte del Cliente @firma. 
 * El Cliente @firma es un applet de libre distribuci�n cuyo c�digo fuente puede ser consultado
 * y descargado desde www.ctt.map.es.
 * Copyright 2009,2010 Gobierno de Espa�a
 * Este fichero se distribuye bajo las licencias EUPL versi�n 1.1  y GPL versi�n 3, o superiores, seg�n las
 * condiciones que figuran en el fichero 'LICENSE.txt' que se acompa�a.  Si se   distribuyera este 
 * fichero individualmente, deben incluirse aqu� las condiciones expresadas all�.
 */


package es.gob.afirma.cliente.utilidades.browser;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

class BrowserDialog extends JDialog
{
    private static final long serialVersionUID = 1L;
    
    private boolean firmar= false;

    private Action afirmar = new AbstractAction()
    {
        private static final long serialVersionUID = 1L;

        {
            putValue(Action.NAME, "Firmar");
        }

        public void actionPerformed(ActionEvent e)
        {
            firmar = true;
            dispose();
        }
    };

    private Action anoFirmar = new AbstractAction()
    {
        private static final long serialVersionUID = 1L;

        {
            putValue(Action.NAME, "No firmar");
        }

        public void actionPerformed(ActionEvent e)
        {
            firmar = false;
            dispose();
        }
    };


    private HTMLEditorKit kit = new HTMLEditorKit()
    {
        private static final long serialVersionUID = 1L;

        @Override
		public Document createDefaultDocument()
        {
            HTMLDocument doc = new AFirmaWebSignHTMLDocument(getStyleSheet());
            doc.setAsynchronousLoadPriority(4);
            doc.setTokenThreshold(100);
            return doc;
        }
    };

    private HyperlinkListener linkListener= new HyperlinkListener()
    {
        public void hyperlinkUpdate(HyperlinkEvent hl)
        {
        	Logger.getLogger("es.gob.afirma").info("DESC: "+hl.getDescription());
            
            if (hl.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED) && hl.getDescription().startsWith("afirma:saveFile("))
            {
                String strId = hl.getDescription().substring(hl.getDescription().indexOf('(') + 1, hl.getDescription().indexOf(')'));
                int id = Integer.parseInt(strId);
                Attachment fich = AFirmaWebSignHTMLDocument.files.get(id);
                Logger.getLogger("es.gob.afirma").info("FICH: "+fich);
                
                int r= JOptionPane.showConfirmDialog(BrowserDialog.this, "�Desea guardar el fichero para comprobar que desea firmarlo?", "Confirmaci\u00F3n", JOptionPane.YES_NO_OPTION);
                if(r==JOptionPane.YES_OPTION)
                {
                	JFileChooser fc = new JFileChooser("\\tmp\\"+fich.getName());
                	fc.setDialogTitle("Seleccione fichero de salida");
                	fc.setDialogType(JFileChooser.SAVE_DIALOG);
                    try
                    {
                    	if(fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    		File outFile = fc.getSelectedFile();
                    		FileOutputStream fos = new FileOutputStream(outFile);
                    		int nBytes;
                    		byte[] buffer = new byte[1024];
                    		InputStream is = fich.getContentInputStream();
                    		while((nBytes = is.read(buffer)) != -1) {
                    			fos.write(buffer, 0, nBytes);
                    		}
                    	}
                    	else {
                    		JOptionPane.showMessageDialog(BrowserDialog.this, "Operaci\u00F3n cancelada por el usuario", "Operaci\u00F3n cancelada por el usuario", JOptionPane.WARNING_MESSAGE);	
                    	}
                    }
                    catch (FileNotFoundException exc)
                    {
                        Logger.getLogger("es.map").severe(exc.toString());
                        JOptionPane.showMessageDialog(BrowserDialog.this, "No se encontr\u00F3 el fichero adjunto", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    catch (IOException exc)
                    {
                        Logger.getLogger("es.map").severe(exc.toString());
                        JOptionPane.showMessageDialog(BrowserDialog.this, "Ocurrio un error al leer el fichero adjunto", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
    };

    public BrowserDialog(String html,Frame parent)
    {
        super(parent, "Firma web", true);
                
        this.getAccessibleContext().setAccessibleParent(parent);
        
//        this.setAlwaysOnTop(true);
//        this.setModal(true);
        JEditorPane ep = new JEditorPane();
        ep.setEditable(false);
        ep.setEnabled(false);
        ep.addHyperlinkListener(linkListener);
        getContentPane().add(new JScrollPane(ep), BorderLayout.CENTER);
        
        setSize(600, 600);
        
        JPanel sur = new JPanel();
        
        sur.add(new JButton(afirmar), BorderLayout.WEST);
        sur.add(new JButton(anoFirmar), BorderLayout.EAST);
        getContentPane().add(sur, BorderLayout.SOUTH);

        ep.setEditorKit(kit);
        try
        {
            Document doc = ep.getDocument();
            kit.read(new StringReader(html), doc, 0);

            disableContent(ep); 
        }
        catch (Exception e)
        {
        	Logger.getLogger("es.map").severe("Ocurrio una excepcion mientras se leia el documento HTML: "+e.getMessage());
        }
    }

    void disableContent(Container cnt) 
    { 
    	int numChildrens = cnt.getComponentCount(); 
    	for(int i=0; i<numChildrens; i++) { 
    		Component children = cnt.getComponent(i); 
    		if(children instanceof Container) 
    			disableContent((Container) children); 
    		children.setEnabled(false); 
    	} 
    	cnt.setEnabled(false); 
    } // disableContent 
    
    @Override
    public void setVisible(boolean b) {
    	JOptionPane.showMessageDialog(this, "Cualquier modificacion NO sera incluida en el documento firmado", "Advertencia", JOptionPane.WARNING_MESSAGE);
    	super.setVisible(b);
    }
    
//	@Override
//	public void show()
//    {
////        if (firmar)
//    	JOptionPane.showMessageDialog(this, "Cualquier modificacion NO sera incluida en el documento firmado", "Advertencia", JOptionPane.WARNING_MESSAGE);
//        //super.show();
//    	setVisible(true);
//        
//    }
    
//    /*
//     * Funciones para expresiones regulares
//     */
//    private String inactiveInputs(String origHtml){
////        String result=origHtml.replaceAll(toIgnoreCase("type='text'"),"TYPE='TEXT' READONLY");
////        result=result.replaceAll("<"+toIgnoreCase("textarea"),"<TEXTAREA READONLY");
////        result=result.replaceAll("<"+toIgnoreCase("select"),"<SELECT READONLY");
////        result=result.replaceAll(toIgnoreCase("type='checkbox'"),"TYPE='CHECKBOX' DISABLED");
////        result=result.replaceAll(toIgnoreCase("type='radio'"),"TYPE='RADIO' DISABLED");
////        result=result.replaceAll(toIgnoreCase("type='password'"),"TYPE='PASSWORD' READONLY");
////        result=result.replaceAll(toIgnoreCase("type='button'"),"TYPE='BUTTON' DISABLED");
//        String result=origHtml.replaceAll(toIgnoreCase("type='text'"),"TYPE='TEXT' READONLY");
//        result=result.replaceAll("<"+toIgnoreCase("textarea"),"<TEXTAREA READONLY");
//        result=result.replaceAll("<"+toIgnoreCase("select"),"<SELECT READONLY");
//        result=result.replaceAll(toIgnoreCase("type='checkbox'"),"TYPE='CHECKBOX' DISABLED");
//        result=result.replaceAll(toIgnoreCase("type='radio'"),"TYPE='RADIO' DISABLED");
//        result=result.replaceAll(toIgnoreCase("type='password'"),"TYPE='PASSWORD' READONLY");
//        result=result.replaceAll(toIgnoreCase("type='button'"),"TYPE='BUTTON' DISABLED");
//        return result;
//    }
//    
//    private String toIgnoreCase(String src){
//        String res="";
//        char[] srcCharArray=src.toCharArray();
//        for (int i=0;i<srcCharArray.length;i++){
//            if (Character.isLetter(srcCharArray[i]))
//                res+="["+Character.toUpperCase(srcCharArray[i])+Character.toLowerCase(srcCharArray[i])+"]";
//            else if (srcCharArray[i]=='\''||srcCharArray[i]=='\"')
//                res+="(\'|\")";
//            else
//                res+=srcCharArray[i];
//        }
//        return res;
//    }
//    
//    private String toParagraph(String src){
//        String res="";
//        if(src.matches("<"+toIgnoreCase("input")+".*"+toIgnoreCase("type='text'")+".*>")){
//            res=src.replaceAll(toIgnoreCase("input"),"P");
//            res=res.replaceAll(toIgnoreCase("type='text'"),"");
//        }else if (src.matches("<"+toIgnoreCase("input")+".*("+toIgnoreCase("type='checkbox'")+"|"+toIgnoreCase("type='radio'")+".*")){
//            res=src.replaceAll(toIgnoreCase("input"),"P");
//            res=res.replaceAll(toIgnoreCase("type='(checkbox|radio)'"),"");
//        }else if()
//        return res;
//    }


    public boolean isFirmar()
    {
        return firmar;
    }
}
