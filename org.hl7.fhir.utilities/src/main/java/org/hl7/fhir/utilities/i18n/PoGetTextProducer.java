package org.hl7.fhir.utilities.i18n;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.utilities.TextFile;
import org.hl7.fhir.utilities.Utilities;

public class PoGetTextProducer extends LanguageFileProducer {

  private int filecount;
  private boolean incLangInFilename;

  public PoGetTextProducer(String rootFolder, String folderName, boolean useLangFolder) {
    super(rootFolder, folderName, useLangFolder);
  }

  public PoGetTextProducer() {
    super();
  }

  @Override
  public LanguageProducerSession startSession(String id, String baseLang) throws IOException {
    return new POGetTextProducerSession(id, baseLang);
  }

  @Override
  public void finish() {
    // nothing
  }

  public class POGetTextProducerSession extends LanguageProducerSession {

    public POGetTextProducerSession(String id, String baseLang) {
      super (id, baseLang);
    }

    @Override
    public LanguageProducerLanguageSession forLang(String targetLang) {
      return new POGetTextLanguageProducerLanguageSession(id, baseLang, targetLang);
    }

    @Override
    public void finish() throws IOException {
      // nothing
    }
  }

  public class POGetTextLanguageProducerLanguageSession extends LanguageProducerLanguageSession {


    private StringBuilder po;

    public POGetTextLanguageProducerLanguageSession(String id, String baseLang, String targetLang) {
      super(id, baseLang, targetLang);
      po = new StringBuilder();
      ln("# "+baseLang+" -> "+targetLang);
      ln("");
    }

    protected void ln(String line) {
      po.append(line+"\r\n");  
    }

    @Override
    public void finish() throws IOException {
      TextFile.stringToFile(po.toString(), getFileName(id, baseLang, targetLang));
      filecount++;
    }

    @Override
    public void entry(TextUnit unit) {
      ln("#: "+unit.getId());
      if (unit.getContext1() != null) {
        ln("#. "+unit.getContext1());
      }
      ln("msgid \""+unit.getSrcText()+"\"");
      ln("msgstr \""+(unit.getTgtText() == null ? "" : unit.getTgtText())+"\"");
      ln("");
    }

  }


  @Override
  public int fileCount() {
    return filecount;
  }

  @Override
  public List<TranslationUnit> loadSource(InputStream source) throws IOException {
    List<TranslationUnit> list = new ArrayList<>();
    InputStreamReader r = new InputStreamReader(source, "UTF-8"); // leave charset out for default
    BufferedReader br = new BufferedReader(r);
    String lang = null;
    String s;
    TranslationUnit tu = null;
    while ((s = Utilities.stripBOM(br.readLine())) != null) {
       if (!Utilities.noString(s)) {
         if (s.trim().startsWith("#")) {
           if (lang == null) {
             String[] p = s.substring(1).trim().split("\\-\\>");
             if (p.length != 2) {
               throw new IOException("Encountered unexpected starting line '"+s+"'");
             } else {
               lang = p[1].trim();
             }
           } else if (s.startsWith("#:")) {
             tu = new TranslationUnit(lang, s.substring(2).trim(), null, null, null);
           } else if (s.startsWith("#.")) {
             if (tu != null) {
               tu.setContext(s.substring(2).trim());
             }
           } else {
             throw new IOException("Encountered unexpected line '"+s+"'");             
           }
       } else if (tu != null && s.startsWith("msgid ")) {
         tu.setSrcText(stripQuotes(s.substring(5).trim()));         
       } else if (tu != null && s.startsWith("msgstr ")) {
         tu.setTgtText(stripQuotes(s.substring(6).trim()));
         if (tu.getTgtText() != null) {
           list.add(tu);
         }
         tu = null;
       } else {
         throw new IOException("Encountered unexpected line '"+s+"'");
       }
       }
    }
    return list;
  }

  private String stripQuotes(String s) {
    if (s.length() <= 2) {
      return null;
    }
    return s.substring(1, s.length()-1);
  }

  private String getFileName(String id, String baseLang, String targetLang) throws IOException {
    return Utilities.path(getRootFolder(), getFolderName(), id+(incLangInFilename ? "-"+baseLang+"-"+targetLang+".po" : ""));
  }

  public boolean isIncLangInFilename() {
    return incLangInFilename;
  }

  public void setIncLangInFilename(boolean incLangInFilename) {
    this.incLangInFilename = incLangInFilename;
  }

  protected void ln(StringBuilder po, String line) {
    po.append(line+"\r\n");  
  }
  
  @Override
  public void produce(String id, String baseLang, String targetLang, List<TranslationUnit> translations, String filename) throws IOException {
    StringBuilder po = new StringBuilder();
    ln(po, "# "+baseLang+" -> "+targetLang);
    ln(po, "");
    for (TranslationUnit tu : translations) {
      ln(po, "#: "+tu.getId());
      if (tu.getContext1() != null) {
        ln(po, "#. "+tu.getContext1());
      }
      if (tu.getOriginal() != null) {
        ln(po, "#| "+tu.getOriginal());
      }
      ln(po, "msgid \""+stripEoln(tu.getSrcText())+"\"");
      ln(po, "msgstr \""+(tu.getTgtText() == null ? "" : stripEoln(tu.getTgtText()))+"\"");
      ln(po, "");
    }
    TextFile.stringToFile(po.toString(), getTargetFileName(targetLang, filename));
  }

  private String stripEoln(String s) {
    s = s.replace("\r\n\r\n", " ").replace("\n\n", " ").replace("\r\r", " ");
    s = s.replace("\r\n", " ").replace("\n", " ").replace("\r", " ");
//    // yes, the double escaping is intentional here - it appears necessary
//    s = s.replace("\\r\\n\\r\\n", " ").replace("\\n\\n", " ").replace("\\r\\r", " ");
//    s = s.replace("\\r\\n", " ").replace("\\n", " ").replace("\\r", " ");
    return s;
  }

  

}
