package com.gmi.nordborglab.browser.client.mvp.handlers;

import com.gwtplatform.mvp.client.UiHandlers;

/**
 * Created with IntelliJ IDEA.
 * User: uemit.seren
 * Date: 1/28/13
 * Time: 8:07 PM
 * To change this template use File | Settings | File Templates.
 */
public interface BasicStudyWizardUiHandlers extends UiHandlers{
    void onCancel();

    void onNext();

    void onPrevious();

    void onShowSelectExperimentPanel();

    void onShowCreateExperimentPanel();

    void onSearchPhenotypeName(String query);

    void onSelectedExperimentChanged();

    void onSaveExperiment(String name, String originator, String design);
}