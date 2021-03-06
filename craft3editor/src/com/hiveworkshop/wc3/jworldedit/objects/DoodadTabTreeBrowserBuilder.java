package com.hiveworkshop.wc3.jworldedit.objects;

import com.hiveworkshop.wc3.jworldedit.objects.sorting.general.SortByDoodadCategoryFolder;
import com.hiveworkshop.wc3.jworldedit.objects.sorting.general.TopLevelCategoryFolder;
import com.hiveworkshop.wc3.resources.WEString;
import com.hiveworkshop.wc3.units.objectdata.War3ID;

public class DoodadTabTreeBrowserBuilder implements ObjectTabTreeBrowserBuilder {

	private static final War3ID DOOD_CATEGORY = War3ID.fromString("dcat");

	@Override
	public TopLevelCategoryFolder build() {
		final TopLevelCategoryFolder root = new TopLevelCategoryFolder(
				new SortByDoodadCategoryFolder(WEString.getString("WESTRING_DE_STANDARDDOODS"), "DoodadCategories",
						DOOD_CATEGORY),
				new SortByDoodadCategoryFolder(WEString.getString("WESTRING_DE_CUSTOMDOODS"), "DoodadCategories",
						DOOD_CATEGORY));
		return root;
	}
}
