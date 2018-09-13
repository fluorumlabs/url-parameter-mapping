package org.vaadin.flow.helper;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

import java.util.UUID;

@Route("")
// Match "12345"
@UrlParameterMapping(":long")
// Match "abcde"
@UrlParameterMapping(":text")
// Match "test/abcde" or "production/abcde" or "test/abcde/12345" or "production/abcde/12345"
@UrlParameterMapping(":mode/:text[/:long]")
// Match "debug/blablabla" or "debug/blabla/bla"
@UrlParameterMapping("debug/:text:.*:")
// Match "uuid/123e4567-e89b-12d3-a456-426655440000"
@UrlParameterMapping("uuid/:uuid")
@IgnoreIfNotMatched
public class DemoView extends HorizontalLayout implements HasUrlParameterMapping, BeforeEnterObserver {
    private TextField patternField;
    private TextField longField;
    private TextField textField;

    private TextField modeField;
    private TextField uuidField;

    @UrlParameter(regEx = "test|production")
    public void setMode(String modeParam) {
        modeField.setEnabled(modeParam != null);
        modeField.setValue(modeParam == null ? "<null>" : modeParam);
    }

    @UrlParameter
    public void setLong(Long longParam) {
        longField.setEnabled(longParam != null);
        longField.setValue(longParam == null ? "<null>" : longParam.toString());
    }

    @UrlParameter
    public void setText(String textParam) {
        textField.setEnabled(textParam != null);
        textField.setValue(textParam == null ? "<null>" : textParam);
    }

    @UrlParameter
    public void setUuid(UUID uuidParam) {
        uuidField.setEnabled(uuidParam != null);
        uuidField.setValue(uuidParam == null ? "<null>" : uuidParam.toString());
    }

    public DemoView() {
        VerticalLayout left = new VerticalLayout();
        VerticalLayout right = new VerticalLayout();
        left.add(new Html("<pre><code>@UrlParameterMapping(\":long\")\n" +
                "@UrlParameterMapping(\":text\")\n" +
                "@UrlParameterMapping(\":mode/:text[/:long]\")\n" +
                "@UrlParameterMapping(\"debug/:text:.*:\")\n" +
                "@UrlParameterMapping(\"uuid/:uuid\")</code></pre>"));

        left.add(new Html("<div>Matching <code>:long</code></div>"));
        left.add(new RouterLink("12345", DemoView.class, "12345"));

        left.add(new Html("<div>Matching <code>:text</code></div>"));
        left.add(new RouterLink("test", DemoView.class, "test"));

        left.add(new Html("<div>Matching <code>:mode:test|production:/:text[/:long]</code></div>"));
        left.add(new RouterLink("test/random", DemoView.class, "test/random"));
        left.add(new RouterLink("test/random/12345", DemoView.class, "test/random/12345"));
        left.add(new RouterLink("production/random", DemoView.class, "production/random"));
        left.add(new RouterLink("production/random/12345", DemoView.class, "production/random/12345"));

        left.add(new Html("<div>Matching <code>debug/:text:.*:</code></div>"));
        left.add(new RouterLink("debug/12345", DemoView.class, "debug/12345"));

        left.add(new Html("<div>Matching <code>uuid/:uuid</code></div>"));
        left.add(new RouterLink("uuid/123e4567-e89b-12d3-a456-426655440000", DemoView.class, "uuid/123e4567-e89b-12d3-a456-426655440000"));

        patternField = new TextField("matched pattern");
        modeField = new TextField("mode");
        longField = new TextField("long");
        textField = new TextField("text");
        uuidField = new TextField("uuid");

        patternField.setReadOnly(true);
        patternField.setWidth("100%");
        modeField.setReadOnly(true);
        modeField.setWidth("100%");
        longField.setReadOnly(true);
        longField.setWidth("100%");
        textField.setReadOnly(true);
        textField.setWidth("100%");
        uuidField.setReadOnly(true);
        uuidField.setWidth("100%");

        right.add(patternField, modeField, longField, textField, uuidField);
        add(left, right);
    }

    /**
     * Method called before navigation to attaching Component chain is made.
     *
     * @param event before navigation event with event details
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        patternField.setValue(getMatchedPattern() == null ? "< no match == null >" : getMatchedPattern());
    }
}
