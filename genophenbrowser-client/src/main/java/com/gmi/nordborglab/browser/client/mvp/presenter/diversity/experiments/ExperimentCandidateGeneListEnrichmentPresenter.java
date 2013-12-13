package com.gmi.nordborglab.browser.client.mvp.presenter.diversity.experiments;

import com.gmi.nordborglab.browser.client.NameTokens;
import com.gmi.nordborglab.browser.client.ParameterizedPlaceRequest;
import com.gmi.nordborglab.browser.client.TabDataDynamic;
import com.gmi.nordborglab.browser.client.events.LoadExperimentEvent;
import com.gmi.nordborglab.browser.client.events.LoadingIndicatorEvent;
import com.gmi.nordborglab.browser.client.gin.ClientModule;
import com.gmi.nordborglab.browser.client.manager.EnrichmentProvider;
import com.gmi.nordborglab.browser.client.manager.EnrichmentProviderImpl;
import com.gmi.nordborglab.browser.client.manager.ExperimentManager;
import com.gmi.nordborglab.browser.client.mvp.presenter.CandidateGeneListEnrichmentPresenter;
import com.gmi.nordborglab.browser.client.mvp.presenter.diversity.meta.CandidateGeneListEnrichmentPresenterWidget;
import com.gmi.nordborglab.browser.client.mvp.presenter.diversity.study.StudyTabPresenter;
import com.gmi.nordborglab.browser.shared.proxy.CandidateGeneListEnrichmentPageProxy;
import com.gmi.nordborglab.browser.shared.proxy.CandidateGeneListEnrichmentProxy;
import com.gmi.nordborglab.browser.shared.proxy.ExperimentProxy;
import com.gmi.nordborglab.browser.shared.proxy.FacetProxy;
import com.gmi.nordborglab.browser.shared.service.CustomRequestFactory;
import com.gmi.nordborglab.browser.shared.util.ConstEnums;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.TabData;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.*;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.client.proxy.TabContentProxyPlace;

import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: uemit.seren
 * Date: 05.12.13
 * Time: 19:33
 * To change this template use File | Settings | File Templates.
 */
public class ExperimentCandidateGeneListEnrichmentPresenter extends CandidateGeneListEnrichmentPresenter<ExperimentCandidateGeneListEnrichmentPresenter.MyProxy> {


    @ProxyCodeSplit
    @NameToken(NameTokens.experimentsEnrichments)
    @TabInfo(container = ExperimentDetailTabPresenter.class,
            label = "Enrichments",
            priority = 2)
    @Title("Enrichments")
    public interface MyProxy extends TabContentProxyPlace<ExperimentCandidateGeneListEnrichmentPresenter> {

    }

    private ExperimentProxy experiment;

    private boolean fireLoadEvent = false;
    private final ExperimentManager experimentManager;

    @Inject
    public ExperimentCandidateGeneListEnrichmentPresenter(final EventBus eventBus, final MyView view, final MyProxy proxy,
                                                          final CustomRequestFactory rf,
                                                          final PlaceManager placeManager,
                                                          final ClientModule.AssistedInjectionFactory factory,
                                                          final ExperimentManager experimentManager
    ) {
        super(eventBus, view, proxy, rf, placeManager, factory, factory.createEnrichmentProvider(EnrichmentProvider.TYPE.EXPERIMENT), ExperimentDetailTabPresenter.TYPE_SetTabContent);
        this.experimentManager = experimentManager;
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, ExperimentDetailTabPresenter.TYPE_SetTabContent,
                this);
    }


    @Override
    protected void onReset() {
        super.onReset();    //To change body of overridden methods use File | Settings | File Templates.
        if (fireLoadEvent) {
            fireEvent(new LoadExperimentEvent(experiment));
            fireLoadEvent = false;
        }
        if (experiment != dataProvider.getEntity()) {
            dataProvider.setEntity(experiment);
            candidateGeneListEnrichmentWidget.refresh();
        }
    }


    @ProxyEvent
    public void onLoadExperiment(LoadExperimentEvent event) {
        experiment = event.getExperiment();
        PlaceRequest request = new ParameterizedPlaceRequest(getProxy().getNameToken()).with("id", experiment.getId().toString());
        String historyToken = placeManager.buildHistoryToken(request);
        TabData tabData = getProxy().getTabData();
        getProxy().changeTab(new TabDataDynamic("Enrichments", tabData.getPriority(), historyToken));
    }

    @Override
    public void prepareFromRequest(PlaceRequest placeRequest) {
        super.prepareFromRequest(placeRequest);
        try {
            final Long experimentIdToLoad = Long.valueOf(placeRequest.getParameter("id", null));
            if (experiment != null && experimentIdToLoad.equals(experiment.getId())) {
                getProxy().manualReveal(ExperimentCandidateGeneListEnrichmentPresenter.this);
                fireLoadEvent = false;
                return;
            }
            LoadingIndicatorEvent.fire(this, true);
            experimentManager.findOne(new Receiver<ExperimentProxy>() {
                @Override
                public void onSuccess(ExperimentProxy response) {
                    LoadingIndicatorEvent.fire(ExperimentCandidateGeneListEnrichmentPresenter.this, false);
                    experiment = response;
                    fireLoadEvent = true;
                    getProxy().manualReveal(ExperimentCandidateGeneListEnrichmentPresenter.this);
                    return;
                }
            }, experimentIdToLoad);
        } catch (NumberFormatException e) {
            getProxy().manualRevealFailed();
            placeManager.revealPlace(new ParameterizedPlaceRequest(NameTokens.experiments));
        }
    }

    @Override
    public boolean useManualReveal() {
        return true;
    }
}
