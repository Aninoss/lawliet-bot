package modules.porn;

import java.util.List;

public class BooruRequest {

    private long guildId;
    private String domain;
    private String searchTerm;
    private String searchTermExtra;
    private String imageTemplate;
    private boolean animatedOnly;
    private boolean canBeVideo;
    private boolean mustBeExplicit;
    private boolean test;
    private List<String> filters;
    private List<String> strictFilters;
    private List<String> skippedResults;
    private boolean premium;
    private int number;

    public BooruRequest setGuildId(long guildId) {
        this.guildId = guildId;
        return this;
    }

    public BooruRequest setDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public BooruRequest setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
        return this;
    }

    public BooruRequest setSearchTermExtra(String searchTermExtra) {
        this.searchTermExtra = searchTermExtra;
        return this;
    }

    public BooruRequest setImageTemplate(String imageTemplate) {
        this.imageTemplate = imageTemplate;
        return this;
    }

    public BooruRequest setAnimatedOnly(boolean animatedOnly) {
        this.animatedOnly = animatedOnly;
        return this;
    }

    public BooruRequest setCanBeVideo(boolean canBeVideo) {
        this.canBeVideo = canBeVideo;
        return this;
    }

    public BooruRequest setMustBeExplicit(boolean mustBeExplicit) {
        this.mustBeExplicit = mustBeExplicit;
        return this;
    }

    public BooruRequest setFilters(List<String> filters) {
        this.filters = filters;
        return this;
    }

    public BooruRequest setStrictFilters(List<String> strictFilters) {
        this.strictFilters = strictFilters;
        return this;
    }

    public BooruRequest setSkippedResults(List<String> skippedResults) {
        this.skippedResults = skippedResults;
        return this;
    }

    public BooruRequest setTest(boolean test) {
        this.test = test;
        return this;
    }

    public BooruRequest setPremium(boolean premium) {
        this.premium = premium;
        return this;
    }

    public BooruRequest setNumber(int number) {
        this.number = number;
        return this;
    }

    public long getGuildId() {
        return guildId;
    }

    public String getDomain() {
        return domain;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public String getSearchTermExtra() {
        return searchTermExtra;
    }

    public String getImageTemplate() {
        return imageTemplate;
    }

    public boolean getAnimatedOnly() {
        return animatedOnly;
    }

    public boolean getCanBeVideo() {
        return canBeVideo;
    }

    public boolean getMustBeExplicit() {
        return mustBeExplicit;
    }

    public boolean getTest() {
        return test;
    }

    public List<String> getFilters() {
        return filters;
    }

    public List<String> getStrictFilters() {
        return strictFilters;
    }

    public List<String> getSkippedResults() {
        return skippedResults;
    }

    public boolean getPremium() {
        return premium;
    }

    public int getNumber() {
        return number;
    }

}