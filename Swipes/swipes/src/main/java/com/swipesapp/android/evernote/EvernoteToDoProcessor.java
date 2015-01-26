
package com.swipesapp.android.evernote;

import com.evernote.edam.type.Note;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class EvernoteToDoProcessor {
    private List<EvernoteToDo> todos;
    private Note note;
    private String updatedContent;
    private OnEvernoteCallback<EvernoteToDoProcessor> callback;
    private boolean needUpdate;

    ////////////////////////////////////////
    // Helper class
    ////////////////////////////////////////
    private class EvernoteHandler extends DefaultHandler {

        private Set<String> startEndElements;
        private StringBuilder tempToDoText;
        private int untitledCount;
        private OnEvernoteCallback<List<EvernoteToDo>> callback;
        private List<EvernoteToDo> todos;
        private boolean checked;
        private boolean insideToDo;

        protected EvernoteHandler()
        {
            // do not call
        }

        public EvernoteHandler(OnEvernoteCallback<List<EvernoteToDo>> callback)
        {
            super();
            this.callback = callback;
            startEndElements = new HashSet<String>(Arrays.asList("div", "br", "table", "tr", "td", "ul", "li", "ol", "en-media", "hr", "en-note"));
            tempToDoText = new StringBuilder();
            untitledCount = 1;
            needUpdate = false;
            insideToDo = false;
        }

        ////////////////////////////////////////
        // XML handling
        ////////////////////////////////////////
        private void finishCurrentToDo()
        {
            if (0 < tempToDoText.length()) {
                // we have some text, now trim it
                String todoText = tempToDoText.toString().trim();
                if (0 == todoText.length()) {
                    // too trimmed (no name)
                    todoText = "Untitled " + untitledCount++;
                }

                // too long?
                if (255 < todoText.length()) {
                    todoText = todoText.substring(0, 255);
                }

                // add to todos
                if (null == todos) {
                    todos = new ArrayList<EvernoteToDo>();
                }
                todos.add(new EvernoteToDo(todoText, checked, todos.size()));

                // reset text
                tempToDoText.setLength(0);
            }
            insideToDo = false;
        }

        private void finishIfNeededForElement(String name)
        {
            if (startEndElements.contains(name.toLowerCase())) {
                finishCurrentToDo();
            }
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes)
        {
            if (localName.equals("en-todo")) {
                finishCurrentToDo();
                String checkedString = attributes.getValue("checked");
                checked = (null != checkedString) ? checkedString.equalsIgnoreCase("true") : false;
                insideToDo = true;
            }
            else if (0 < tempToDoText.length()) {
                finishIfNeededForElement(localName);
            }
        }

        public void endElement(String uri, String localName, String qName)
        {
            if (0 < tempToDoText.length()) {
                finishIfNeededForElement(localName);
            }
        }

        public void characters(char[] ch, int start, int length)
        {
            if (insideToDo)
                tempToDoText.append(ch, start, length);
        }

        public void endDocument()
        {
            this.callback.onSuccess(todos);
        }

        public void fatalError(SAXParseException e)
        {
            this.callback.onException(e);
        }

    }

    ////////////////////////////////////////
    // main methods
    ////////////////////////////////////////
    public static void createInstance(String noteRefString, final OnEvernoteCallback<EvernoteToDoProcessor> callback)
    {
        EvernoteToDoProcessor evernoteToDoProcessor = new EvernoteToDoProcessor();
        evernoteToDoProcessor.doCreateInstance(noteRefString, callback);
    }

    private void doCreateInstance(String noteRefString, final OnEvernoteCallback<EvernoteToDoProcessor> callback)
    {
        this.callback = callback;
        this.todos = new ArrayList<EvernoteToDo>();
        EvernoteIntegration.getInstance().downloadNote(noteRefString, new OnEvernoteCallback<Note>() {
            @Override
            public void onSuccess(Note data) {
                EvernoteToDoProcessor.this.note = data;
                parseAndLoadTodos();
            }

            @Override
            public void onException(Exception e) {
                callback.onException(e);
            }
        });
    }

    public void saveToEvernote(final OnEvernoteCallback<Boolean> callback)
    {
        if (null == updatedContent)
            callback.onException(new Exception("Note not updated"));

        EvernoteIntegration.getInstance().updateNote(note, new OnEvernoteCallback<Note>() {
            @Override
            public void onSuccess(Note data) {
                EvernoteToDoProcessor.this.note = note;
                callback.onSuccess(true);
            }

            @Override
            public void onException(Exception e) {
                callback.onException(e);
            }
        });
    }

    private void parseAndLoadTodos()
    {
        this.todos.clear();
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            String s = note.getContent();
            parser.parse(new InputSource(new StringReader(note.getContent())), new EvernoteHandler(new OnEvernoteCallback<List<EvernoteToDo>>() {
                @Override
                public void onSuccess(List<EvernoteToDo> data) {
                    EvernoteToDoProcessor.this.todos = data;
                    callback.onSuccess(EvernoteToDoProcessor.this);
                }

                @Override
                public void onException(Exception e) {
                    callback.onException(e);
                }
            })
            );
        } catch (Exception e) {
            callback.onException(e);
        }
    }

    public List<EvernoteToDo> getToDos()
    {
        return this.todos;
    }

    public boolean getNeedUpdate()
    {
        return this.needUpdate;
    }

    ////////////////////////////////////////
    // updating TODOs
    ////////////////////////////////////////
    public boolean updateToDo(EvernoteToDo updatedToDo, boolean checked)
    {
        if ((null != updatedToDo) && (updatedToDo.isChecked() != checked)) {
            if (null == updatedContent)
                updatedContent = note.getContent();

            int startLocation = 0;
            for (int i = 0; i <= updatedToDo.getPosition(); i++) {
                int newLocation = updatedContent.indexOf("<en-todo", startLocation);
                if (-1 == newLocation)
                    return false;
                startLocation = newLocation + 8;
            }

            int endLocation = startLocation;
            while (('>' != updatedContent.charAt(endLocation)) && ('/' != updatedContent.charAt(endLocation))) {
                if (++endLocation >= updatedContent.length()) {
                    return false;
                }
            }

            String replaceString = " checked=\"" + (checked ? "true" : "false") + "\"";
            updatedContent = updatedContent.substring(0, startLocation) + replaceString + updatedContent.substring(endLocation);
            needUpdate = true;
            return true;
        }

        return false;
    }

    public boolean updateToDo(EvernoteToDo updatedToDo, String title)
    {
        if ((null != updatedToDo) && (!updatedToDo.getTitle().equals(title))) {
            if (null == updatedContent)
                updatedContent = note.getContent();

            int startLocation = 0;
            for (int i = 0; i <= updatedToDo.getPosition(); i++) {
                int newLocation = updatedContent.indexOf("<en-todo", startLocation);
                if (-1 == newLocation)
                    return false;
                startLocation = newLocation + 8;
            }

            String escapedOldTitle = xmlEscape(updatedToDo.getTitle());
            int newLocation = updatedContent.indexOf(escapedOldTitle, startLocation);
            if (-1 == newLocation)
                return false;
            startLocation = newLocation;
            if (startLocation + escapedOldTitle.length() > updatedContent.length()) {
                return false;
            }

            updatedContent = updatedContent.substring(0, startLocation) + xmlEscape(title) + updatedContent.substring(startLocation + escapedOldTitle.length());
            needUpdate = true;
            return true;
        }

        return false;
    }

    private String xmlEscape(String xml)
    {
        StringBuilder escapedXML = new StringBuilder();
        for (int i = 0; i < xml.length(); i++) {
            char c = xml.charAt(i);
            switch (c) {
                case '<':
                    escapedXML.append("&lt;");
                    break;
                case '>':
                    escapedXML.append("&gt;");
                    break;
                case '\"':
                    escapedXML.append("&quot;");
                    break;
                case '&':
                    escapedXML.append("&amp;");
                    break;
                //case '\'':
                //    escapedXML.append("&#x27;");
                //    break;
                default:
                    escapedXML.append(c);
            }
        }

        return escapedXML.toString();
    }

    ////////////////////////////////////////
    // adding TODOs
    ////////////////////////////////////////
    private int getNewToDoPosAtTheBeginning()
    {
        int devPos = updatedContent.indexOf("<en-note");
        if (-1 != devPos) {
            int loc = updatedContent.indexOf('>', devPos + 8);
            return (-1 != loc) ? loc + 1 : loc;
        }
        return devPos;
    }

    private int getNewToDoPos()
    {
        if (0 < todos.size()) {
            EvernoteToDo todo = todos.get(todos.size() - 1);

            int startLocation = 0;
            for (int i = 0; i <= todo.getPosition(); i++) {
                int newLocation = updatedContent.indexOf("<en-todo", startLocation);
                if (-1 == newLocation)
                    return getNewToDoPosAtTheBeginning();
                startLocation = newLocation + 8;
            }

            String escapedTitle = xmlEscape(todo.getTitle());
            int newLocation = updatedContent.indexOf(escapedTitle, startLocation);
            if (-1 == newLocation)
                return getNewToDoPosAtTheBeginning();

            int result = newLocation + escapedTitle.length();
            newLocation = updatedContent.indexOf("</div>", result);
            if (-1 != newLocation) {
                return newLocation + 6; // 6 is the length of </div>
            }
            return result;
        }
        return getNewToDoPosAtTheBeginning();
    }

    public boolean addToDo(String title)
    {
        if (null == updatedContent)
            updatedContent = note.getContent();

        int startPos = getNewToDoPos();
        if (startPos >= updatedContent.length()) {
            // TODO store error
            return false;
        }

        if (-1 != startPos) {
            updatedContent = updatedContent.substring(0, startPos) + "<div><en-todo/>" + xmlEscape(title) + "<br/></div>" + updatedContent.substring(startPos);
            needUpdate = true;
            return true;
        }

        return false;
    }
}
