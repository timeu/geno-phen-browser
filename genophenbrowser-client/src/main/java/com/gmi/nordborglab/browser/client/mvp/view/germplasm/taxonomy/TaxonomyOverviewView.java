package com.gmi.nordborglab.browser.client.mvp.view.germplasm.taxonomy;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.gmi.nordborglab.browser.client.mvp.handlers.TaxonomyOverviewUiHandlers;
import com.gmi.nordborglab.browser.client.resources.MainResources;
import com.gmi.nordborglab.browser.client.ui.card.ExperimentCard;
import com.gmi.nordborglab.browser.client.ui.card.TaxonomyCard;
import com.gmi.nordborglab.browser.client.ui.card.TransformationCard;
import com.gmi.nordborglab.browser.shared.proxy.StatisticTypeProxy;
import com.gmi.nordborglab.browser.shared.proxy.TaxonomyProxy;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Maps;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.gwtplatform.mvp.client.ViewImpl;
import com.gmi.nordborglab.browser.client.mvp.presenter.germplasm.taxonomy.TaxonomyOverviewPresenter;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewWithUiHandlers;

import java.util.List;

public class TaxonomyOverviewView extends ViewWithUiHandlers<TaxonomyOverviewUiHandlers> implements
		TaxonomyOverviewPresenter.MyView {

	private final Widget widget;
    @UiField
    FlowPanel container;

    private ImmutableBiMap<TransformationCard,TaxonomyProxy> cardToProxyMap;

    private final MainResources mainRes;

    public interface Binder extends UiBinder<Widget, TaxonomyOverviewView> {
	}

    public class TaxonomyCardClickHandler implements  ClickHandler{

        @Override
        public void onClick(ClickEvent event) {
            Object source = event.getSource();
            if (source instanceof FocusPanel) {
                TaxonomyCard card = (TaxonomyCard) ((FocusPanel) source).getParent();
                getUiHandlers().onClickTaxonomy(cardToProxyMap.get(card));
            }
        }
    }
    private TaxonomyCardClickHandler taxonomyCardClickHandler= new TaxonomyCardClickHandler();

	@Inject
	public TaxonomyOverviewView(final Binder binder, final MainResources mainRes) {
        this.mainRes = mainRes;
		widget = binder.createAndBindUi(this);

	}

	@Override
	public Widget asWidget() {
		return widget;
	}


    @Override
    public void setTaxonomies(List<TaxonomyProxy> taxonomies) {
       ImmutableBiMap.Builder builder = ImmutableBiMap.<TaxonomyCard,TaxonomyProxy>builder();
       container.clear();
       int i =0 ;
       for (TaxonomyProxy taxonomy:taxonomies) {
           TaxonomyCard taxonomyCard = new TaxonomyCard();
           container.add(taxonomyCard);
           builder.put(taxonomyCard,taxonomy);
           ImageResource imageRes = null;
           if (taxonomy.getSpecies().equals("thaliana")) {
               imageRes = mainRes.getThalianaImage();
           }
           else {
               imageRes = mainRes.getLyrataImage();
           }
           taxonomyCard.setTaxonomy(taxonomy,imageRes);
           taxonomyCard.setClickhandler(taxonomyCardClickHandler);
       }
       cardToProxyMap = builder.build();
    }
}
