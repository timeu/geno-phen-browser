package com.gmi.nordborglab.browser.server.service.impl;

import java.io.*;
import java.util.*;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import com.gmi.nordborglab.browser.server.domain.phenotype.*;
import com.gmi.nordborglab.browser.server.domain.stats.AppStat;
import com.gmi.nordborglab.browser.server.domain.acl.AppUser;
import com.gmi.nordborglab.browser.server.domain.cdv.Transformation;
import com.gmi.nordborglab.browser.server.domain.stats.DateStatHistogram;
import com.gmi.nordborglab.browser.server.domain.stats.DateStatHistogramFacet;
import com.gmi.nordborglab.browser.server.domain.util.NewsItem;
import com.gmi.nordborglab.browser.server.domain.util.Publication;
import com.gmi.nordborglab.browser.server.domain.util.UserNotification;
import com.gmi.nordborglab.browser.server.math.Transformations;
import com.gmi.nordborglab.browser.server.repository.*;
import com.gmi.nordborglab.browser.server.rest.PhenotypeUploadData;
import com.gmi.nordborglab.browser.server.rest.PhenotypeUploadValue;
import com.gmi.nordborglab.browser.server.security.EsAclManager;
import com.gmi.nordborglab.browser.server.security.SecurityUtil;
import com.gmi.nordborglab.browser.shared.proxy.AppStatProxy;
import com.gmi.nordborglab.browser.shared.proxy.DateStatHistogramFacetProxy;
import com.gmi.nordborglab.browser.shared.proxy.DateStatHistogramProxy;
import com.gmi.nordborglab.browser.shared.proxy.TransformationDataProxy;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.elasticsearch.action.search.MultiSearchRequestBuilder;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.facet.FacetBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.Facets;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gmi.nordborglab.browser.server.domain.AppData;
import com.gmi.nordborglab.browser.server.domain.BreadcrumbItem;
import com.gmi.nordborglab.browser.server.domain.cdv.Study;
import com.gmi.nordborglab.browser.server.domain.cdv.StudyProtocol;
import com.gmi.nordborglab.browser.server.domain.genotype.AlleleAssay;
import com.gmi.nordborglab.browser.server.domain.germplasm.Passport;
import com.gmi.nordborglab.browser.server.domain.germplasm.Sampstat;
import com.gmi.nordborglab.browser.server.domain.germplasm.Stock;
import com.gmi.nordborglab.browser.server.domain.germplasm.Taxonomy;
import com.gmi.nordborglab.browser.server.domain.observation.Experiment;
import com.gmi.nordborglab.browser.server.service.HelperService;
import com.gmi.nordborglab.browser.shared.service.HelperFactory;
import com.google.common.collect.Iterables;
import com.google.web.bindery.autobean.vm.AutoBeanFactorySource;
import org.supercsv.cellprocessor.CellProcessorAdaptor;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseLong;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.cellprocessor.ift.StringCellProcessor;
import org.supercsv.exception.SuperCsvCellProcessorException;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.util.CsvContext;

@Service
@Transactional(readOnly = true)
public class HelperServiceImpl implements HelperService {

    private static HelperFactory helperFactory = AutoBeanFactorySource.create(HelperFactory.class);


    private static class ParsePhenotypeValue extends CellProcessorAdaptor {

        public static enum PHENOTYPE_VALUE_TYPES {
            MEASURE, MEAN, STD, VARIANCE, MODE, MEDIAN, COUNT;
        }

        private ParsePhenotypeValue() {
            super();
        }

        private ParsePhenotypeValue(CellProcessor next) {
            super(next);
        }

        @Override
        public Object execute(Object value, CsvContext context) {
            validateInputNotNull(value, context);
            for (PHENOTYPE_VALUE_TYPES type : PHENOTYPE_VALUE_TYPES.values()) {
                if (type.name().equalsIgnoreCase(value.toString())) {
                    return next.execute(value, context);
                }
            }
            throw new SuperCsvCellProcessorException(String.format("Could not parse '%s' as a phenotype value type", value), context, this);
        }
    }


    @Resource
    private UserRepository userRepository;

    @Resource
    private ExperimentRepository experimentRepository;

    @Resource
    private UserNotificationRepository userNotificationRepository;

    @Resource
    private StockRepository stockRepository;

    @Resource
    private TaxonomyRepository taxonomRepository;

    @Resource
    private PassportRepository passportRepository;

    @Resource
    private SampstatRepository sampstatRepository;

    @Resource
    private AlleleAssayRepository alleleAssayRepository;

    @Resource
    private StudyProtocolRepository studyProtocolRepository;

    @Resource
    private UnitOfMeasureRepository unitOfMeasureRepository;

    @Resource
    private StatisticTypeRepository statisticTypeRepository;

    @Resource
    private TransformationRepository transformationRepository;

    @Resource
    private TraitUomRepository traitUomRepository;

    @Resource
    private StudyRepository studyRepository;

    @Resource
    private PublicationRepository publicationRepository;

    @Resource
    private NewsRepository newsRepository;

    @Resource
    private Client client;

    @Resource
    protected EsAclManager esAclManager;


    @Override
    public List<BreadcrumbItem> getBreadcrumbs(Long id, String object) {
        List<BreadcrumbItem> breadcrumbs = new ArrayList<BreadcrumbItem>();
        if (object.equals("experiment") || object.equals("phenotypes")) {
            Experiment exp = experimentRepository.findOne(id);
            breadcrumbs.add(new BreadcrumbItem(exp.getId(), exp.getName(), "experiment"));
        } else if (object.equals("phenotype")) {
            Experiment exp = experimentRepository.findByPhenotypeId(id);
            TraitUom traitUom = traitUomRepository.findOne(id);
            breadcrumbs.add(new BreadcrumbItem(exp.getId(), exp.getName(), "experiment"));
            breadcrumbs.add(new BreadcrumbItem(traitUom.getId(), traitUom.getLocalTraitName(), "phenotype"));
        } else if (object.equals("study")) {
            Study study = studyRepository.findOne(id);
            TraitUom trait = Iterables.get(study.getTraits(), 0).getTraitUom();
            Experiment exp = Iterables.get(study.getTraits(), 0).getObsUnit().getExperiment();
            breadcrumbs.add(new BreadcrumbItem(exp.getId(), exp.getName(), "experiment"));
            breadcrumbs.add(new BreadcrumbItem(trait.getId(), trait.getLocalTraitName(), "phenotype"));
            breadcrumbs.add(new BreadcrumbItem(study.getId(), study.getName(), "study"));
        } else if (object.equals("studywizard")) {
            Experiment exp = experimentRepository.findByPhenotypeId(id);
            TraitUom trait = traitUomRepository.findOne(id);
            breadcrumbs.add(new BreadcrumbItem(exp.getId(), exp.getName(), "experiment"));
            breadcrumbs.add(new BreadcrumbItem(trait.getId(), trait.getLocalTraitName(), "phenotype"));
            breadcrumbs.add(new BreadcrumbItem(trait.getId(), "New Study", "studywizard"));
        } else if (object.equals("taxonomy")) {
            Taxonomy tax = taxonomRepository.findOne(id);
            breadcrumbs.add(new BreadcrumbItem(tax.getId(), tax.getGenus() + " " + tax.getSpecies(), "taxonomy"));
        } else if (object.equals("passports")) {
            Taxonomy tax = taxonomRepository.findOne(id);
            breadcrumbs.add(new BreadcrumbItem(tax.getId(), tax.getGenus() + " " + tax.getSpecies(), "passports"));
        } else if (object.equals("passport")) {
            Passport passport = passportRepository.findOne(id);
            Taxonomy taxonomy = passport.getTaxonomy();
            breadcrumbs.add(new BreadcrumbItem(taxonomy.getId(), taxonomy.getGenus() + " " + taxonomy.getSpecies(), "taxonomy"));
            breadcrumbs.add(new BreadcrumbItem(passport.getId(), passport.getAccename(), "passport"));
        } else if (object.equals("stock")) {
            Stock stock = stockRepository.findOne(id);
            Passport passport = stock.getPassport();
            Taxonomy taxonomy = passport.getTaxonomy();
            breadcrumbs.add(new BreadcrumbItem(taxonomy.getId(), taxonomy.getGenus() + " " + taxonomy.getSpecies(), "taxonomy"));
            breadcrumbs.add(new BreadcrumbItem(passport.getId(), passport.getAccename(), "passport"));
            breadcrumbs.add(new BreadcrumbItem(stock.getId(), stock.getId().toString(), "stock"));
        } else if (object.equalsIgnoreCase("publication")) {
            Publication publication = publicationRepository.findOne(id);
            breadcrumbs.add(new BreadcrumbItem(publication.getId(), publication.getFirstAuthor() + " - " + publication.getTitle(), "publication"));
        }
        return breadcrumbs;
    }


	/*@Override
    public String getAppData() {
		List<UnitOfMeasure> unitOfMeasureValues = unitOfMeasureRepository.findAll();
		List<StatisticType> statisticTypeValues = statisticTypeRepository.findAll();
		String json;
		AutoBean<AppDataProxy> bean = helperFactory.appData();
		List<UnitOfMeasureProxy> unitOfMeasures = new ArrayList<UnitOfMeasureProxy>();
		List<StatisticTypeProxy> statisticTypes = new ArrayList<StatisticTypeProxy>();
		UnitOfMeasureProxy unitOfMeasureProxy = helperFactory.unitOfMeasure().as();
		unitOfMeasureProxy.setUnitType("");
		unitOfMeasures.add(unitOfMeasureProxy);
		for (UnitOfMeasure unitOfMeasure:unitOfMeasureValues) {
			unitOfMeasureProxy = helperFactory.unitOfMeasure().as();
			unitOfMeasureProxy.setId(unitOfMeasure.getId());
			unitOfMeasureProxy.setUnitType(unitOfMeasure.getUnitType());
			unitOfMeasures.add(unitOfMeasureProxy);
		}
		//bean.setUnitOfMeasureList(unitOfMeasures);
		for (StatisticType statisticType:statisticTypeValues) {
			StatisticTypeProxy statisticTypeProxy = helperFactory.statisticType().as();
			statisticTypeProxy.setId(statisticType.getId());
			statisticTypeProxy.setStatType(statisticType.getStatType());
			statisticTypes.add(statisticTypeProxy);
		}
		//bean.setStatisticTypeList(statisticTypes);
		//String key = ser.serialize(someProxy);
		// Create the flattened representation
		//String payload = store.encode();
		json = AutoBeanCodex.encode(bean).getPayload();
		return json;
		
	}*/


    @Override
    public AppData getAppData() {
        List<UnitOfMeasure> unitOfMeasureValues = unitOfMeasureRepository.findAll();
        List<StatisticType> statisticTypeValues = statisticTypeRepository.findAll();
        List<AlleleAssay> alleleAssayValues = alleleAssayRepository.findAll();
        List<StudyProtocol> studyProtocolValues = studyProtocolRepository.findAll();
        List<Sampstat> sampStatValues = sampstatRepository.findAll();
        List<Transformation> transformationValues = transformationRepository.findAll();
        List<UserNotification> userNotifications = getUserNotifications(10);
        Page<NewsItem> page = newsRepository.findAll(new PageRequest(0, 10));

        AppData appData = new AppData();
        appData.setStatisticTypeList(statisticTypeValues);
        appData.setUnitOfMeasureList(unitOfMeasureValues);
        appData.setAlleleAssayList(alleleAssayValues);
        appData.setStudyProtocolList(studyProtocolValues);
        appData.setSampStatList(sampStatValues);
        appData.setTransformationList(transformationValues);
        appData.setUserNotificationList(userNotifications);
        appData.setStats(getAppStats());
        appData.setNews(page.getContent());
        return appData;
    }


    @Override
    public PhenotypeUploadData getPhenotypeUploadData(byte[] csvData) throws IOException {
        PhenotypeUploadData data = new PhenotypeUploadData();
        ICsvListReader metaInformationReader = null;
        ICsvListReader valueHeaderReader = null;
        ICsvListReader valueReader = null;
        boolean hasMetaInfo = false;
        try {
            metaInformationReader = new CsvListReader(new InputStreamReader(new ByteArrayInputStream(csvData)), CsvPreference.STANDARD_PREFERENCE);
            String[] valueHeader = null;
            final String[] metaHeader = metaInformationReader.getHeader(true);
            hasMetaInfo = (metaHeader.length > 0 && metaHeader[0].equals("#HEADER"));
            if (hasMetaInfo) {
                final Map<String, String> metaInfo = getMetaInformationFromHeader(metaHeader);
                updatePhenotypeUploadDataWithMetaInformation(data, metaInfo);
                valueHeader = metaInformationReader.getHeader(false);
            } else {
                valueHeader = metaHeader;
            }


            final int columnCount = valueHeader.length;
            data.setValueHeader(Arrays.asList(valueHeader).subList(1, valueHeader.length));

            CellProcessor[] valueHeaderCellProccessors = createValueHeaderCellProcessors(columnCount);
            CellProcessor[] valueCellProcessors = createValueCellProcessors(columnCount);

            valueHeaderReader = new CsvListReader(new InputStreamReader(new ByteArrayInputStream(csvData)), CsvPreference.STANDARD_PREFERENCE);
            if (hasMetaInfo)
                valueHeaderReader.getHeader(true);
            valueHeaderReader.read(valueHeaderCellProccessors);

            valueReader = new CsvListReader(new InputStreamReader(new ByteArrayInputStream(csvData)), CsvPreference.STANDARD_PREFERENCE);
            if (hasMetaInfo)
                valueReader.getHeader(true);
            valueReader.getHeader(false);

            List<String> phenotypeValues = null;
            while ((phenotypeValues = valueReader.read()) != null) {
                PhenotypeUploadValue value = parseAndCheckPhenotypeValue(phenotypeValues);
                if (value != null)
                    data.addPhenotypeValue(value);
            }
            data.sortByErrors();

        } catch (SuperCsvCellProcessorException e) {
            data.setErrorMessage(String.format("Error parsing header. '%s'", e.getMessage()));
        } catch (Exception e) {
            data.setErrorMessage("General error reading csv file");
        } finally {
            if (metaInformationReader != null)
                metaInformationReader.close();
        }

        return data;
    }

    @Override
    public List<TransformationData> calculateTransformations(List<Double> values) {
        List<TransformationData> transformations = Lists.newArrayList();
        transformations.add(new TransformationData(TransformationDataProxy.TYPE.LOG, Transformations.logTransform(values)));
        transformations.add(new TransformationData(TransformationDataProxy.TYPE.SQRT, Transformations.sqrtTransform(values)));
        transformations.add(new TransformationData(TransformationDataProxy.TYPE.BOXCOX, Transformations.boxCoxTransform(values)));
        return transformations;
    }

    @Override
    public List<UserNotification> getUserNotifications(Integer limit) {
        AppUser appUser = null;
        try {
            appUser = userRepository.findOne(Long.parseLong(SecurityUtil.getUsername()));
        } catch (Exception e) {

        }

        if (appUser == null)
            return null;
        List<UserNotification> notifications = userNotificationRepository.findByAppUserIdOrAppUserIsNullOrderByIdDesc(Long.parseLong(SecurityUtil.getUsername()));
        return filterUserNotifications(notifications, appUser.getNotificationCheckDate(), limit);
    }

    private static List<UserNotification> filterUserNotifications(List<UserNotification> notifications, Date modificationCheckDate, Integer limit) {
        List<UserNotification> recentNotifications = Lists.newArrayList();
        for (int i = 0; i < notifications.size(); i++) {
            UserNotification notification = notifications.get(i);
            if (!notification.isRead(modificationCheckDate) || limit == null || (limit != null && limit > recentNotifications.size())) {
                recentNotifications.add(notification);
            }
        }
        return recentNotifications;
    }


    private PhenotypeUploadValue parseAndCheckPhenotypeValue(List<String> phenotypeValues) {
        boolean hasError = false;
        boolean isIdKnown = false;
        PhenotypeUploadValue parsedValue = new PhenotypeUploadValue();
        try {
            String sourceId = phenotypeValues.get(0);
            parsedValue.setValues(phenotypeValues.subList(1, phenotypeValues.size()));
            parsedValue.setSourceId(sourceId);
            Long id = Long.parseLong(sourceId);
            Long passportId = null;
            Long stockId = null;
            String accessionName = null;
            if (passportRepository.exists(id)) {
                Passport passport = passportRepository.findOne(id);
                passportId = id;
                isIdKnown = true;
                accessionName = passport.getAccename();

            } else if (stockRepository.exists(id)) {
                Stock stock = stockRepository.findOne(id);
                stockId = id;
                isIdKnown = true;
                passportId = stock.getPassport().getId();
                accessionName = stock.getPassport().getAccename();
            }
            parsedValue.setAccessionName(accessionName);
            parsedValue.setStockId(stockId);
            parsedValue.setPassportId(passportId);
        } catch (Exception e) {
            hasError = true;
        }
        parsedValue.setParseError(hasError);
        parsedValue.setIdKnown(isIdKnown);
        return parsedValue;
    }

    private void updatePhenotypeUploadDataWithMetaInformation(PhenotypeUploadData data, Map<String, String> metaInfo) {
        if (metaInfo.containsKey("name"))
            data.setName(metaInfo.get("name"));
        //TODO retrieve unitofMeasureFromText
        if (metaInfo.containsKey("unitofmeasure"))
            data.setUnitOfMeasure(metaInfo.get("unitofmeasure"));
        if (metaInfo.containsKey("protocol"))
            data.setProtocol(metaInfo.get("protocol"));
        if (metaInfo.containsKey("traitontology"))
            data.setTraitOntology(metaInfo.get("traitontology"));
        if (metaInfo.containsKey("environmentontology"))
            data.setEnvironmentOntology(metaInfo.get("environmentontology"));
    }

    private static Map<String, String> getMetaInformationFromHeader(String[] header) {
        Map<String, String> metaInfo = new HashMap<String, String>();
        for (int i = 1; i < header.length; i++) {
            String[] metaSplit = header[i].split("=");
            metaInfo.put(metaSplit[0].toLowerCase(), metaSplit[1]);
        }
        return metaInfo;
    }

    private static CellProcessor[] createValueHeaderCellProcessors(int valueColumns) {
        StringCellProcessor valueHeaderCell = new NotNull(new ParsePhenotypeValue());
        CellProcessor[] processors = new CellProcessor[valueColumns];
        processors[0] = null;
        for (int i = 1; i < valueColumns; i++) {
            processors[i] = valueHeaderCell;
        }
        return processors;
    }

    private static CellProcessor[] createValueCellProcessors(int valueColumns) {
        StringCellProcessor valueCell = new NotNull(new ParseDouble());
        CellProcessor[] processors = new CellProcessor[valueColumns];
        processors[0] = new NotNull(new ParseLong());
        for (int i = 1; i < valueColumns; i++) {
            processors[i] = valueCell;
        }
        //TODO properly implement parse issues on the backend.
        return null;

    }

    private UnitOfMeasure getUnitOfMeasureFromText(String text) {
        List<UnitOfMeasure> unitOfmeasures = unitOfMeasureRepository.findAll();
        for (UnitOfMeasure unitOfMeasure : unitOfmeasures) {
            if (unitOfMeasure.getUnitType().equalsIgnoreCase(text))
                return unitOfMeasure;
        }
        return null;
    }

    @Override
    public List<AppStat> getAppStats() {
        List<AppStat> stats = Lists.newArrayList();
        MultiSearchRequestBuilder requestBuilder = client.prepareMultiSearch();
        // user count
        requestBuilder.add(getStatsSearchBuilder().setTypes("user"));

        FilterBuilder filter = esAclManager.getAclFilter(Lists.newArrayList("read"));
        //  get studies
        requestBuilder.add(getStatsSearchBuilder().setTypes("experiment").setFilter(filter));
        requestBuilder.add(getStatsSearchBuilder().setTypes("phenotype").setFilter(filter));
        requestBuilder.add(getStatsSearchBuilder().setTypes("study").setFilter(filter));
        requestBuilder.add(getStatsSearchBuilder().setTypes("publication"));

        MultiSearchResponse multiResponse = requestBuilder.execute().actionGet();

        stats.add(new AppStat(AppStatProxy.STAT.USER, multiResponse.getResponses()[0].getResponse().getHits().getTotalHits()));
        stats.add(new AppStat(AppStatProxy.STAT.STUDY, multiResponse.getResponses()[1].getResponse().getHits().getTotalHits()));
        stats.add(new AppStat(AppStatProxy.STAT.PHENOTYPE, multiResponse.getResponses()[2].getResponse().getHits().getTotalHits()));
        stats.add(new AppStat(AppStatProxy.STAT.ANALYSIS, multiResponse.getResponses()[3].getResponse().getHits().getTotalHits()));
        stats.add(new AppStat(AppStatProxy.STAT.PUBLICATION, multiResponse.getResponses()[4].getResponse().getHits().getTotalHits()));
        stats.add(new AppStat(AppStatProxy.STAT.ONTOLOGY, 2));
        return stats;
    }

    private SearchRequestBuilder getStatsSearchBuilder() {
        return client.prepareSearch(esAclManager.getIndex()).setSearchType(SearchType.COUNT);
    }

    @Override
    public List<DateStatHistogramFacet> findRecentTraitHistogram(DateStatHistogramProxy.INTERVAL interval) {
        List<DateStatHistogramFacet> histogram = Lists.newArrayList();
        FacetBuilder facet = FacetBuilders.dateHistogramFacet("recent").field("published").interval(interval.name().toLowerCase());
        FilterBuilder filter = esAclManager.getAclFilter(Lists.newArrayList("read"));

        MultiSearchRequestBuilder requestBuilder = client.prepareMultiSearch();

        // experiments
        requestBuilder.add(
                client.prepareSearch(esAclManager.getIndex()).setTypes("experiment")
                        .addFacet(facet)
                        .setQuery(QueryBuilders.constantScoreQuery(filter))
                        .setSize(0)
        );
        //phenotypes
        requestBuilder.add(
                client.prepareSearch(esAclManager.getIndex()).setTypes("phenotype")
                        .addFacet(facet)
                        .setQuery(QueryBuilders.constantScoreQuery(filter))
                        .setSize(0)
        );

        //study
        requestBuilder.add(
                client.prepareSearch(esAclManager.getIndex()).setTypes("study")
                        .addFacet(facet)
                        .setQuery(QueryBuilders.constantScoreQuery(filter))
                        .setSize(0)
        );

        MultiSearchResponse multiResponse = requestBuilder.execute().actionGet();
        histogram.add(new DateStatHistogramFacet(getHistogram(multiResponse.getResponses()[0].getResponse().getFacets(), interval), DateStatHistogramFacetProxy.TYPE.study));
        histogram.add(new DateStatHistogramFacet(getHistogram(multiResponse.getResponses()[1].getResponse().getFacets(), interval), DateStatHistogramFacetProxy.TYPE.phenotype));
        histogram.add(new DateStatHistogramFacet(getHistogram(multiResponse.getResponses()[2].getResponse().getFacets(), interval), DateStatHistogramFacetProxy.TYPE.analysis));
        return histogram;
    }

    @Override
    public Study applyTransformation(Study study) {
        DescriptiveStatistics stats = Transformations.getDescriptiveStatistics(Collections2.transform(study.getTraits(), new Function<Trait, Double>() {

            @Nullable
            @Override
            public Double apply(@Nullable Trait trait) {
                Double value = null;
                try {
                    value = Double.parseDouble(trait.getValue());
                } catch (Exception e) {

                }
                return value;
            }
        }));
        Transformations.TransformFunc transFormFunc = Transformations.getTransformFunc(study.getTransformation().getName(), stats.getMin(), stats.getVariance());
        if (transFormFunc != null) {
            for (Trait trait : study.getTraits()) {
                try {
                    Double value = Double.parseDouble(trait.getValue());
                    trait.setValue(transFormFunc.apply(value).toString());
                } catch (Exception e) {

                }
            }
        }
        return study;
    }


    private List<DateStatHistogram> getHistogram(Facets facets, DateStatHistogramProxy.INTERVAL interval) {
        DateHistogramFacet searchFacet = (DateHistogramFacet) facets.facetsAsMap().get("recent");
        List<DateStatHistogram> dates = Lists.newArrayList();
        for (DateHistogramFacet.Entry termEntry : searchFacet) {
            dates.add(new DateStatHistogram(new Date(termEntry.getTime()), termEntry.getCount(), interval));
        }
        return dates;
    }

}
