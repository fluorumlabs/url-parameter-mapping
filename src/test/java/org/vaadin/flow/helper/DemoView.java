package org.vaadin.flow.helper;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("")
// Match "12345"
@UrlParameterMapping(":long")
// Match "abcde"
@UrlParameterMapping(":text")
// Match "test/abcde" or "production/abcde" or "test/abcde/12345" or "production/abcde/12345"
@UrlParameterMapping(":mode:test|production:/:text[/:long]")
// Match "debug/blablabla" or "debug/blabla/bla"
@UrlParameterMapping("debug/:text:.*:")
public class DemoView extends VerticalLayout implements HasUrlParameterMapping {

    private TextField longField;
    private TextField textField;
    private TextField modeField;

    public DemoView() {
        add(new Html("<pre><code>@UrlParameterMapping(\":long\")\n" +
                "@UrlParameterMapping(\":text\")\n" +
                "@UrlParameterMapping(\":mode:test|production:/:text[/:long]\")\n" +
                "@UrlParameterMapping(\"debug/:text:.*:\")</code></pre>"));

        add(new Html("<div>Matching <code>:long</code></div>"));
        add(new RouterLink("12345", DemoView.class, "12345"));

        add(new Html("<div>Matching <code>:text</code></div>"));
        add(new RouterLink("test", DemoView.class, "test"));

        add(new Html("<div>Matching <code>:mode:test|production:/:text[/:long]</code></div>"));
        add(new RouterLink("test/random", DemoView.class, "test/random"));
        add(new RouterLink("test/random/12345", DemoView.class, "test/random/12345"));
        add(new RouterLink("production/random", DemoView.class, "production/random"));
        add(new RouterLink("production/random/12345", DemoView.class, "production/random/12345"));

        add(new Html("<div>Matching <code>debug/:text:.*:</code></div>"));
        add(new RouterLink("debug/12345", DemoView.class, "debug/12345"));

        add(new Hr());

        add(new Html("<div>No matches</div>"));
        add(new RouterLink("something/random", DemoView.class, "something/random"));
        add(new RouterLink("test/random/text", DemoView.class, "test/random/text"));

        add(new Hr());

        modeField = new TextField("mode");
        longField = new TextField("long");
        textField = new TextField("text");

        modeField.setReadOnly(true);
        longField.setReadOnly(true);
        textField.setReadOnly(true);

        add(modeField, longField, textField);
    }

    public void setMode(String modeParam) {
        modeField.setEnabled(modeParam != null);
        modeField.setValue(modeParam == null ? "<null>" : modeParam);
    }

    public void setLong(Long longParam) {
        longField.setEnabled(longParam != null);
        longField.setValue(longParam == null ? "<null>" : longParam.toString());
    }

    public void setText(String textParam) {
        textField.setEnabled(textParam != null);
        textField.setValue(textParam == null ? "<null>" : textParam);
    }
}
