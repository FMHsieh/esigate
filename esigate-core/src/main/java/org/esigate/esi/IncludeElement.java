package org.esigate.esi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.esigate.Driver;
import org.esigate.DriverFactory;
import org.esigate.HttpErrorPage;
import org.esigate.Renderer;
import org.esigate.ResourceContext;
import org.esigate.parser.ElementType;
import org.esigate.parser.ParserContext;
import org.esigate.regexp.ReplaceRenderer;
import org.esigate.util.UriUtils;
import org.esigate.vars.VariablesResolver;
import org.esigate.xml.XpathRenderer;
import org.esigate.xml.XsltRenderer;

class IncludeElement extends BaseElement {
	private static final String PROVIDER_PATTERN = "$(PROVIDER{";

	public final static ElementType TYPE = new BaseElementType("<esi:include", "</esi:include") {
		public IncludeElement newInstance() {
			return new IncludeElement();
		}

	};

	private final Appendable outAdapter = new Appendable() {

		public Appendable append(CharSequence csq, int start, int end) throws IOException {
			IncludeElement.this.characters(csq, start, end);
			return this;
		}

		public Appendable append(char c) throws IOException {
			return append(new StringBuilder(1).append(c), 0, 1);
		}

		public Appendable append(CharSequence csq) throws IOException {
			return append(csq, 0, csq.length());
		}
	};
	private StringBuilder buf;
	private Map<String, CharSequence> fragmentReplacements;
	private Map<String, CharSequence> regexpReplacements;
	private Tag includeTag;
	private boolean write = false;

	IncludeElement() {
	}

	@Override
	public void characters(CharSequence csq, int start, int end) {
		if (write)
			buf.append(csq, start, end);
	}

	@Override
	public void onTagEnd(String tag, ParserContext ctx) throws IOException, HttpErrorPage {
		write = true;
		String src = includeTag.getAttribute("src");
		String alt = includeTag.getAttribute("alt");
		boolean ignoreError = "continue".equals(includeTag.getAttribute("onerror"));
		try {
			processPage(src, includeTag, ctx);
		} catch (IOException e) {
			if (alt != null) {
				processPage(alt, includeTag, ctx);
			} else if (!ignoreError && !ctx.reportError(e)) {
				throw e;
			}
		} catch (HttpErrorPage e) {
			if (alt != null) {
				processPage(alt, includeTag, ctx);
			} else if (!ignoreError && !ctx.reportError(e)) {
				throw e;
			}
		}
		// apply regexp replacements
		if (!regexpReplacements.isEmpty()) {
			for (Entry<String, CharSequence> entry : regexpReplacements.entrySet()) {
				buf = new StringBuilder(Pattern.compile(entry.getKey()).matcher(buf).replaceAll(entry.getValue().toString()));
			}
		}

		// write accumulated data into parent
		ctx.getCurrent().characters(buf, 0, buf.length());

		buf = null;
		fragmentReplacements = null;
		regexpReplacements = null;
	}

	@Override
	protected void parseTag(Tag tag, ParserContext ctx) throws IOException, HttpErrorPage {
		buf = new StringBuilder();
		fragmentReplacements = new HashMap<String, CharSequence>();
		regexpReplacements = new HashMap<String, CharSequence>();
		includeTag = tag;
	}

	void processPage(String src, Tag tag, ParserContext ctx) throws IOException, HttpErrorPage {
		String fragment = tag.getAttribute("fragment");
		String xpath = tag.getAttribute("xpath");
		String xslt = tag.getAttribute("stylesheet");
		boolean noStore = "on".equalsIgnoreCase(tag.getAttribute("no-store"));
		String ttl = tag.getAttribute("ttl");
		String maxWait = tag.getAttribute("maxwait");
		boolean rewriteAbsoluteUrl = "true".equalsIgnoreCase(tag.getAttribute("rewriteabsoluteurl"));

		ResourceContext resourceContext = ctx.getResourceContext();
		List<Renderer> rendererList = new ArrayList<Renderer>();
		Driver driver;
		String page;

		if (maxWait != null) {
			try {
				resourceContext.getOriginalRequest().setFetchMaxWait(Integer.parseInt(maxWait));
			} catch (NumberFormatException e) {
				// invalid maxwait value
			}
		}

		if (resourceContext != null) {
			resourceContext.getOriginalRequest().setNoStoreResource(noStore);

			if (!noStore && ttl != null) {
				String timePeriod = ttl.substring(ttl.length() - 1);
				Long time = null;
				try {
					time = Long.parseLong(ttl.substring(0, ttl.length() - 1));
					// convert time to milliseconds
					if (timePeriod.equalsIgnoreCase("d")) {
						time = time * 86400000;
					} else if (timePeriod.equalsIgnoreCase("h")) {
						time = time * 3600000;
					} else if (timePeriod.equalsIgnoreCase("m")) {
						time = time * 60000;
					} else if (timePeriod.equalsIgnoreCase("s")) {
						time = time * 1000;
					}
				} catch (NumberFormatException e) {
					// Invalid time, ttl is null
				}
				resourceContext.getOriginalRequest().setResourceTtl(time);
			}
		}

		int idx = src.indexOf(PROVIDER_PATTERN);
		if (idx < 0) {
			page = src;
			driver = ctx.getResourceContext().getDriver();
		} else {
			int startIdx = idx + PROVIDER_PATTERN.length();
			int endIndex = src.indexOf("})", startIdx);
			String provider = src.substring(startIdx, endIndex);
			page = src.substring(endIndex + "})".length());
			driver = DriverFactory.getInstance(provider);
		}

		if (rewriteAbsoluteUrl) {
			Map<String, String> replaceRules = new HashMap<String, String>();
			String baseUrl = resourceContext.getBaseURL();
			String visibleBaseUrl = driver.getConfiguration().getVisibleBaseURL(baseUrl);

			String contextBaseUrl;
			String contextVisibleBaseUrl;
			contextBaseUrl = UriUtils.createUri(baseUrl).getPath();
			if (visibleBaseUrl != null && !visibleBaseUrl.equals("") && !baseUrl.equals(visibleBaseUrl)) {
				contextVisibleBaseUrl = UriUtils.createUri(visibleBaseUrl).getPath();
				replaceRules.put("href=(\"|')" + visibleBaseUrl + "(.*)(\"|')", "href=$1" + contextVisibleBaseUrl + "$2$3");
				replaceRules.put("src=(\"|')" + visibleBaseUrl + "(.*)(\"|')", "src=$1" + contextVisibleBaseUrl + "$2$3");
				replaceRules.put("href=(\"|')" + baseUrl + "(.*)(\"|')", "href=$1" + contextBaseUrl + "$2$3");
				replaceRules.put("src=(\"|')" + baseUrl + "(.*)(\"|')", "src=$1" + contextBaseUrl + "$2$3");
			} else {
				contextBaseUrl = UriUtils.createUri(baseUrl).getPath();
				replaceRules.put("href=(\"|')" + baseUrl + "(.*)(\"|')", "href=$1" + contextBaseUrl + "$2$3");
				replaceRules.put("src=(\"|')" + baseUrl + "(.*)(\"|')", "src=$1" + contextBaseUrl + "$2$3");
			}

			rendererList.add(new ReplaceRenderer(replaceRules));
		}

		page = VariablesResolver.replaceAllVariables(page, resourceContext.getOriginalRequest());
		InlineCache ic = InlineCache.getFragment(src);
		if (ic != null && !ic.isExpired()) {
			String cache = ic.getFragment();
			characters(cache, 0, cache.length());
		} else {
			EsiRenderer esiRenderer;
			if (fragment != null)
				esiRenderer = new EsiRenderer(page, fragment);
			else
				esiRenderer = new EsiRenderer();
			if (fragmentReplacements != null && !fragmentReplacements.isEmpty())
				esiRenderer.setFragmentsToReplace(fragmentReplacements);
			rendererList.add(esiRenderer);
			if (xpath != null) {
				rendererList.add(new XpathRenderer(xpath));
			} else if (xslt != null) {
				rendererList.add(new XsltRenderer(xslt, driver, resourceContext));
			}
			driver.render(page, outAdapter, resourceContext, rendererList.toArray(new Renderer[rendererList.size()]));
		}
	}

	void addFragmentReplacement(String fragment, CharSequence replacement) {
		fragmentReplacements.put(fragment, replacement);
	}

	void addRegexpReplacement(String regexp, CharSequence replacement) {
		regexpReplacements.put(regexp, replacement);
	}

}
