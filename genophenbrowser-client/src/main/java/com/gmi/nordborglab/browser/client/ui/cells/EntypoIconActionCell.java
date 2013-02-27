package com.gmi.nordborglab.browser.client.ui.cells;

import com.gmi.nordborglab.browser.client.mvp.view.diversity.tools.GWASViewerView;
import com.google.gwt.cell.client.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safecss.shared.SafeStylesBuilder;
import com.google.gwt.safecss.shared.SafeStylesUtils;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;
import com.google.gwt.user.client.Element;


/**
 * Created with IntelliJ IDEA.
 * User: uemit.seren
 * Date: 2/25/13
 * Time: 10:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class EntypoIconActionCell<C> extends AbstractCell<C> {

    private final String entypoString;
    private final ActionCell.Delegate<C> delegate;

    interface Template extends SafeHtmlTemplates {

        @SafeHtmlTemplates.Template("<span style=\"{0}\">{1}</span>")
        SafeHtml cell(SafeStyles styles,SafeHtml icon);

    }
    private static Template templates = GWT.create(Template.class);

    public EntypoIconActionCell(final String entypoString,ActionCell.Delegate<C> delegate) {
        super("click");
        this.entypoString = entypoString;
        this.delegate = delegate;
    }


    @Override
    public void onBrowserEvent(Context context, com.google.gwt.dom.client.Element parent, C value, NativeEvent event, ValueUpdater<C> valueUpdater) {
        super.onBrowserEvent(context, parent, value, event, valueUpdater);    //To change body of overridden methods use File | Settings | File Templates.
        if ("click".equals(event.getType())) {
            EventTarget eventTarget = event.getEventTarget();
            if (parent.getFirstChildElement().isOrHasChild(Element.as(eventTarget))) {
                delegate.execute(value);
            }
        }
    }

    @Override
    public void render(Context context, C value, SafeHtmlBuilder sb) {
        SafeStylesBuilder builder = new SafeStylesBuilder();
        builder.append(SafeStylesUtils.fromTrustedNameAndValue("font-family", "EntypoRegular"));
        builder.append(SafeStylesUtils.forFontSize(38, Style.Unit.PX));
        builder.append(SafeStylesUtils.forMarginRight(15, Style.Unit.PX));
        builder.append(SafeStylesUtils.forCursor(Style.Cursor.POINTER));
        SafeHtml rendered = templates.cell(builder.toSafeStyles(), SafeHtmlUtils.fromTrustedString(entypoString));
        sb.append(rendered);
    }
}