package com.gmi.nordborglab.browser.shared.service;


import com.gmi.nordborglab.browser.shared.proxy.ExperimentPageProxy;
import com.gmi.nordborglab.browser.shared.proxy.ExperimentProxy;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.ServiceName;

@ServiceName(value="com.gmi.nordborglab.browser.server.service.ExperimentService",locator="com.gmi.nordborglab.browser.server.service.SpringServiceLocator")
public interface ExperimentRequest extends RequestContext {

	Request<ExperimentPageProxy> findByAcl(int start, int size);
	Request<ExperimentProxy> findExperiment(Long id);
	Request<ExperimentProxy> save(ExperimentProxy experiment);
}
