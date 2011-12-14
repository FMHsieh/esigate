package org.esigate.esi;

import org.esigate.parser.ElementType;

abstract class BaseElementType implements ElementType {
	private final String startTag;
	private final String endTag;

	protected BaseElementType(String startTag, String endTag) {
		this.startTag = startTag;
		this.endTag = endTag;
	}

	public final boolean isStartTag(String tag) {
		return tag.startsWith(startTag);
	}

	public final boolean isEndTag(String tag) {
		return tag.startsWith(endTag);
	}

}
