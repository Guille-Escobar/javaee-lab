## Copyright 2015 JAXIO http://www.jaxio.com
##
## Licensed under the Apache License, Version 2.0 (the "License");
## you may not use this file except in compliance with the License.
## You may obtain a copy of the License at
##
##    http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing, software
## distributed under the License is distributed on an "AS IS" BASIS,
## WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
## See the License for the specific language governing permissions and
## limitations under the License.
##
$output.java($WebModelSupport, "GenericController")##

$output.requireStatic($WebConversation, "ConversationHolder.getCurrentConversation")##
$output.requireStatic("com.google.common.base.Throwables.propagate")##
$output.requireStatic("com.google.common.base.Preconditions.checkNotNull")##
$output.requireStatic("com.google.common.collect.Lists.newArrayList")##
$output.requireStatic("org.apache.commons.lang.StringUtils.isBlank")##
$output.require("java.io.Serializable")##
$output.require("java.util.List")##
$output.require("java.util.logging.Logger")##
$output.require("com.google.common.base.Splitter")##
$output.require("javax.faces.component.UIComponent")##
$output.require("javax.faces.context.FacesContext")##
$output.require("javax.inject.Inject")##
$output.require("org.apache.commons.beanutils.PropertyUtils")##
$output.require("org.apache.commons.lang.WordUtils")##
$output.require($RepositorySupport, "JpaUniqueUtil")##
$output.require($RepositorySupport, "SearchParameters")##
#if($project.hibernateSearchUsed)
$output.require($RepositorySupport, "TermSelector")##
#end
$output.require($ModelSupport, "Identifiable")##
$output.require($PrinterSupport, "GenericPrinter")##
$output.require($RepositorySupport, "GenericRepository")##
$output.require($WebUtil, "MessageUtil")##
$output.require($WebConversation, "ConversationCallBack")##
$output.require($WebConversation, "ConversationContext")##
$output.require($WebConversation, "ConversationManager")##
$output.require($WebPermissionSupport, "GenericPermission")##

/**
 * Base controller for JPA entities providing helper methods to:
 * <ul>
 *  <li>start conversations</li>
 *  <li>create conversation context</li>
 *  <li>support autoComplete component</li>
 *  <li>perform actions</li>
 *  <li>support excel export</li>
 * </ul>
 */
public abstract class ${output.currentClass}<E extends Identifiable<PK>, PK extends Serializable> {
    private static final String PERMISSION_DENIED = "/error/accessdenied";
    private String selectUri;
    private String editUri;

    @Inject
    private Logger log;
    @Inject
    protected ConversationManager conversationManager;
    @Inject
    protected JpaUniqueUtil jpaUniqueUtil;
    @Inject
    protected MessageUtil messageUtil;
#if($project.hibernateSearchUsed)
    @Inject
    protected MetamodelUtil metamodelUtil;
#end
    protected GenericRepository<E, PK> repository;
    protected GenericPermission<E> permission;
    protected GenericPrinter<E> printer;

    public void init(GenericRepository<E, PK> repository, GenericPermission<E> permission, GenericPrinter<E> printer, String selectUri, String editUri) {
        this.repository = checkNotNull(repository);
        this.permission = checkNotNull(permission);
        this.printer = checkNotNull(printer);
        this.selectUri = checkNotNull(selectUri);
        this.editUri = checkNotNull(editUri);
    }

    public GenericRepository<E, PK> getRepository() {
        return repository;
    }

    public GenericPermission<E> getPermission() {
        return permission;
    }

    public MessageUtil getMessageUtil() {
        return messageUtil;
    }

    public String getPermissionDenied() {
        return PERMISSION_DENIED;
    }

    public String getSelectUri() {
        return selectUri;
    }

    public String getEditUri() {
        return editUri;
    }

    // ----------------------------------------
    // START CONVERSATION PROGRAMATICALY
    // ----------------------------------------

    /**
     * Start a new {@link Conversation} that allows the user to search an existing entity.
     * This method can be invoked from an h:commandLink's action attribute.
     * @return the implicit first view for the newly created conversation.
     */
    public String beginSearch() {
        if (!permission.canSearch()) {
            return getPermissionDenied();
        }
        return beginConversation(newSearchContext(getSearchLabelKey()));
    }

    /**
     * Start a new {@link Conversation} that allows the user to create a new entity.
     * This method can be invoked from an h:commandLink's action attribute.
     * @return the implicit first view for the newly created conversation.
     */
    public String beginCreate() {
        if (!permission.canCreate()) {
            return getPermissionDenied();
        }
        return beginConversation(newEditContext(getCreateLabelKey(), repository.getNewWithDefaults()));
    }

    /**
     * Start a new {@link Conversation} using the passed ctx as the first conversation context.
     * @return the implicit first view for the newly created conversation.
     */
    public String beginConversation(ConversationContext<E> ctx) {
        return conversationManager.beginConversation(ctx).nextView();
    }

    // ----------------------------------------
    // AUTO COMPLETE SUPPORT  
    // ----------------------------------------

    /**
     * Auto-complete support. This method is used by primefaces autoComplete component.
     */
    public List<E> complete(String value) {
        try {
            SearchParameters searchParameters = new SearchParameters() //
                .limitBroadSearch() //
                .distinct() //
                .orMode();
        E template = repository.getNew();
        for (String property : completeProperties()) {
#if($project.hibernateSearchUsed)
$output.require($RepositorySupport, "MetamodelUtil")##
            if (repository.isIndexed(property)) {
                    searchParameters.addTerm(new TermSelector(metamodelUtil.toAttribute(property, repository.getType())).selected(value));
            } else {
                PropertyUtils.setProperty(template, property, value);
            }
#else
            PropertyUtils.setProperty(template, property, value);
#end
        }
        return repository.find(template, searchParameters);
        } catch(Exception e) {
            log.warning("error during complete: " + e.getMessage());
            throw propagate(e);
        }
    }

    protected Iterable<String> completeProperties() {
        String completeOnProperties = parameter("completeOnProperties", String.class);
        return isBlank(completeOnProperties) ? printer.getDisplayedAttributes() : Splitter.on(";,").omitEmptyStrings().split(completeOnProperties);
    }

    public List<String> completeProperty(String value) {
        return completeProperty(value, parameter("property", String.class), parameter("maxResults", Integer.class));
    }

    public List<String> completeProperty(String value, String property) {
        return completeProperty(value, property, null);
    }

    public List<String> completeProperty(String toMatch, String property, Integer maxResults) {
#if($project.hibernateSearchUsed)
        List<String> values = newArrayList();
        if (repository.isIndexed(property)) {
            values.addAll(completePropertyUsingFullText(toMatch, property, maxResults));
        } else {
            values.addAll(completePropertyInDatabase(toMatch, property, maxResults));
        }
#else
        List<String> values = completePropertyInDatabase(toMatch, property, maxResults);
#end
        if (isBlank(toMatch) || values.contains(toMatch)) {
            // the term is already in the results, return them directly
            return values;
        } else {
            // add the term before the results as it is not part of the results
            List<String> retWithValue = newArrayList(toMatch);
            retWithValue.addAll(values);
            return retWithValue;
        }
    }
#if($project.hibernateSearchUsed)
$output.require($RepositorySupport, "MetamodelUtil")##

    protected List<String> completePropertyUsingFullText(String term, String property, Integer maxResults) {
        try {
            SearchParameters searchParameters = new SearchParameters().limitBroadSearch().distinct();
            searchParameters.addTerm(new TermSelector(metamodelUtil.toAttribute(property, repository.getType())).selected(term));
            if (maxResults != null) {
                searchParameters.setMaxResults(maxResults);
            }
            return repository.findProperty(String.class, repository.getNew(), searchParameters, property);
        } catch(Exception e) {
            log.warning("error during completePropertyUsingFullText: " + e.getMessage());
            throw propagate(e);
        }
    }
#end

    protected List<String> completePropertyInDatabase(String value, String property, Integer maxResults) {
        try {
            SearchParameters searchParameters = new SearchParameters() //
                .limitBroadSearch() //
                .caseInsensitive() //
                .anywhere() //
                .distinct();
            if (maxResults != null) {
                searchParameters.setMaxResults(maxResults);
            }
        E template = repository.getNew();
        PropertyUtils.setProperty(template, property, value);
            return repository.findProperty(String.class, template, searchParameters, property);
        } catch(Exception e) {
            log.warning("error during completePropertyInDatabase: " + e.getMessage());
            throw propagate(e);
        }
    }

    /**
     * A simple autoComplete that returns exactly the input. It is used in search forms with {@link PropertySelector}.
     */
    public List<String> completeSame(String value) {
        return newArrayList(value);
    }

    @SuppressWarnings("unchecked")
    protected <T> T parameter(String propertyName, Class<T> expectedType) {
        return (T) UIComponent.getCurrentComponent(FacesContext.getCurrentInstance()).getAttributes().get(propertyName);
    }

    protected SearchParameters defaultOrder(SearchParameters searchParameters) {
        return searchParameters;
    }

    /**
     * Decision helper used when handling ajax autoComplete event and regular page postback.
     */
    public boolean shouldReplace(E currentEntity, E selectedEntity) {
        if (currentEntity == selectedEntity) {
            return false;
        }

        if (currentEntity != null && selectedEntity != null && currentEntity.isIdSet() && selectedEntity.isIdSet()) {
            if (selectedEntity.getId().equals(currentEntity.getId())) {
                Comparable<Object> currentVersion = repository.getVersion(currentEntity);
                if (currentVersion == null) {
                    // assume no version at all is available
                    // let's stick with current entity.
                    return false;
                }
                Comparable<Object> selectedVersion = repository.getVersion(selectedEntity);
                if (currentVersion.compareTo(selectedVersion) == 0) {
                    // currentEntity could have been edited and not yet saved, we keep it.
                    return false;
                } else {
                    // we could have an optimistic locking exception at save time
                    // TODO: what should we do here?
                    return false;
                }
            }
        }
        return true;
    }

    // ----------------------------------------
    // CREATE IMPLICIT EDIT VIEW
    // ----------------------------------------

    /**
     * Helper to create a new {@link ConversationContext} to view the passed entity and set it as the current conversation's next context.  
     * The vars <code>sub</code> <code>readonly</code> are set to true.
     * The permission {@link GenericPermission${pound}canView()} is checked.
     * 
     * @param labelKey label key for breadCrumb and conversation menu.
     * @param e the entity to view.
     * @return the implicit view to access this context.
     */
    public String editSubReadOnlyView(String labelKey, E e) {
        return editReadOnlyView(labelKey, e, true);
    }
    
    public String editReadOnlyView(String labelKey, E e, boolean sub) {
        if (!permission.canView(e)) {
            return getPermissionDenied();
        }
        ConversationContext<E> ctx = newEditContext(labelKey, e).sub(sub).readonly();
        return getCurrentConversation().nextContext(ctx).view();
    }

    /**
     * Helper to create a new {@link ConversationContext} to edit the passed entity and set it as the current conversation's next context.  
     * The var <code>sub</code> is set to true.
     * The permission {@link GenericPermission${pound}canEdit()} is checked.
     * 
     * @param labelKey label key for breadCrumb and conversation menu.
     * @param e the entity to edit.
     * @return the implicit view to access this context.
     */
    public String editSubView(String labelKey, E e, ConversationCallBack<E> editCallBack) {
        return editView(labelKey, e, editCallBack, true);
    }
    
    public String editView(String labelKey, E e, ConversationCallBack<E> editCallBack, boolean sub) {
        if (!permission.canEdit(e)) {
            return getPermissionDenied();
        }
        ConversationContext<E> ctx = newEditContext(labelKey, e, editCallBack).sub(sub);
        return getCurrentConversation().nextContext(ctx).view();
    }

    /**
     * Helper to create a new {@link ConversationContext} to create a new entity and set it as the current conversation's next context.  
     * The var <code>sub</code> is set to true.
     * 
     * @param labelKey label key for breadCrumb and conversation menu.
     * @return the implicit view to access this context.
     */
    public String createSubView(String labelKey, ConversationCallBack<E> createCallBack) {
        return createView(labelKey, createCallBack, true);
    }

    /**
     * Helper to create a new {@link ConversationContext} to edit the passed new entity and set it as the current conversation's next context.  
     * The var <code>sub</code> is set to true.
     * The permission {@link GenericPermission${pound}canCreate()} is checked.
     * 
     * @param labelKey label key for breadCrumb and conversation menu.
     * @param e the entity to edit.
     * @return the implicit view to access this context.
     */
    public String createSubView(String labelKey, E e, ConversationCallBack<E> createCallBack) {
        return createView(labelKey, e, createCallBack, true);
    }
    
    public String createView(String labelKey, ConversationCallBack<E> createCallBack, boolean sub) {
        return createView(labelKey, repository.getNewWithDefaults(), createCallBack, sub);
    }
    
    public String createView(String labelKey, E e, ConversationCallBack<E> createCallBack, boolean sub) {
        if (!permission.canCreate()) {
            return getPermissionDenied();
        }
        ConversationContext<E> ctx = newEditContext(labelKey, e, createCallBack).sub(sub);
        return getCurrentConversation().nextContext(ctx).view();
    }

    // ----------------------------------------
    // CREATE IMPLICIT SELECT VIEW
    // ----------------------------------------

    public String selectSubView(String labelKey, ConversationCallBack<E> selectCallBack) {
        return selectView(labelKey, selectCallBack, true);
    }
    
    public String selectView(String labelKey, ConversationCallBack<E> selectCallBack, boolean sub) {
        if (!permission.canSelect()) {
            return getPermissionDenied();
        }
        ConversationContext<E> ctx = newSearchContext(labelKey, selectCallBack).sub(sub);
        return getCurrentConversation().nextContext(ctx).view();
    }

    public String multiSelectSubView(String labelKey, ConversationCallBack<E> selectCallBack) {
        return multiSelectView(labelKey, selectCallBack, true);
    }
    
    public String multiSelectView(String labelKey, ConversationCallBack<E> selectCallBack, boolean sub) {
        if (!permission.canSelect()) {
            return getPermissionDenied();
        }
        ConversationContext<E> ctx = newSearchContext(labelKey, selectCallBack).sub(sub);
        ctx.setVar("multiCheckboxSelection", true);
        return getCurrentConversation().nextContext(ctx).view();
    }

    // ----------------------------------------
    // CREATE EDIT CONVERSATION CONTEXT
    // ----------------------------------------

    /**
     * Helper to construct a new {@link ConversationContext} to edit an entity.
     * @param e the entity to edit.
     */
    public ConversationContext<E> newEditContext(E e) {
        return new ConversationContext<E>().entity(e).isNewEntity(!e.isIdSet()).viewUri(getEditUri());
    }

    public ConversationContext<E> newEditContext(String labelKey, E e) {
        return newEditContext(e).labelKey(labelKey);
    }

    public ConversationContext<E> newEditContext(String labelKey, E e, ConversationCallBack<E> callBack) {
        return newEditContext(labelKey, e).callBack(callBack);
    }

    // ----------------------------------------
    // CREATE SEARCH CONVERSATION CONTEXT
    // ----------------------------------------

    /**
     * Helper to construct a new {@link ConversationContext} for search/selection.
     */
    public ConversationContext<E> newSearchContext() {
        return new ConversationContext<E>(getSelectUri());
    }

    public ConversationContext<E> newSearchContext(String labelKey) {
        return newSearchContext().labelKey(labelKey);
    }

    public ConversationContext<E> newSearchContext(String labelKey, ConversationCallBack<E> callBack) {
        return newSearchContext(labelKey).callBack(callBack);
    }

    // ----------------------------------------
    // ACTIONS INVOKED FORM THE VIEW
    // ----------------------------------------

    public ConversationContext<E> getSelectedContext(E selected) {
        return newEditContext(getEditUri(), selected);
    }

    /**
     * Action to create a new entity.
     */
    public String create() {
        if (!permission.canCreate()) {
            return getPermissionDenied();
        }
        E newEntity = repository.getNewWithDefaults();
        ConversationContext<E> ctx = getSelectedContext(newEntity).labelKey(getCreateLabelKey());
        return getCurrentConversation().nextContext(ctx).view();
    }

    /**
     * Support for {@link GenericLazyDataModel.${pound}edit()} and {@link GenericLazyDataModel${pound}onRowSelect(org.primefaces.event.SelectEvent)} methods 
     */
    public String edit(E entity) {
        if (!permission.canEdit(entity)) {
            return getPermissionDenied();
        }
        ConversationContext<E> ctx = getSelectedContext(entity).labelKey(getEditLabelKey(), printer.print(entity));
        return getCurrentConversation().nextContext(ctx).view();
    }

    /**
     * Support for the {@link GenericLazyDataModel.${pound}view()} method
     */
    public String view(E entity) {
        if (!permission.canView(entity)) {
            return getPermissionDenied();
        }
        ConversationContext<E> ctx = getSelectedContext(entity).sub().readonly().labelKey(getViewLabelKey(), printer.print(entity));
        return getCurrentConversation().nextContext(ctx).view();
    }

    /**
     * Return the print friendly view for the passed entity. I can be invoked directly from the view.
     */
    public String print(E entity) {
        if (!permission.canView(entity)) {
            return getPermissionDenied();
        }
        ConversationContext<E> ctx = getSelectedContext(entity).readonly().print().labelKey(getViewLabelKey(), printer.print(entity));
        return getCurrentConversation().nextContext(ctx).view();
    }
    
    protected String select(E entity) {
        if (!permission.canSelect()) {
            return getPermissionDenied();
        }
        return getCurrentConversation() //
                .<ConversationContext<E>> getCurrentContext() //
                .getCallBack() //
                .selected(entity);
    }

    protected String getSearchLabelKey() {
        return getLabelName()  + "_search";
    }

    protected String getCreateLabelKey() {
        return getLabelName()  + "_create";
    }

    protected String getEditLabelKey() {
        return getLabelName()  + "_edit";
    }

    protected String getViewLabelKey() {
        return getLabelName() + "_view";
    }

    protected String getLabelName() {
        return WordUtils.uncapitalize(getEntityName());
    }

    private String getEntityName() {
        return repository.getType().getSimpleName();
    }
}
