/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader.toc;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ListView;

import org.fbreader.common.android.FBActivity;
import org.fbreader.common.android.FBReaderUtil;
import org.fbreader.reader.*;
import org.fbreader.reader.android.ContextMenuDialog;
import org.fbreader.reader.android.MainActivity;
import org.fbreader.reader.android.TOCAdapterBase;
import org.fbreader.util.android.ViewUtil;

import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.fbreader.FBReaderApp;

import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;

import org.geometerplus.zlibrary.ui.android.R;

public class TOCActivity extends FBActivity {
	private final BookCollectionShadow myCollection = new BookCollectionShadow();

	private TOCAdapter myAdapter;
	private TOCTree mySelectedItem;

	@Override
	protected int layoutId() {
		return R.layout.toc;
	}

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		final Intent intent = getIntent();

		final Book book = FBReaderIntents.getBookExtra(intent, myCollection);
		if (book == null) {
			finish();
			return;
		}

		FBReaderUtil.setBookTitle(this, book);

		final FBReaderApp fbreader = (FBReaderApp)FBReaderApp.Instance();
		final TOCTree root = fbreader.Model.TOCTree;

		myAdapter = new TOCAdapter((ListView)findViewById(R.id.toc_list), root);
		final int ref = intent.getIntExtra(MainActivity.TOCKey.REF, -1);
		final TOCTree treeToSelect =
			ref != -1 ? TOCTreeUtil.findTreeByReference(root, ref) : null;
		myAdapter.selectItem(treeToSelect);
		mySelectedItem = treeToSelect;
	}

	private static final int PROCESS_TREE_ITEM_ID = 0;
	private static final int READ_BOOK_ITEM_ID = 1;

	private final class TOCAdapter extends TOCAdapterBase {
		TOCAdapter(ListView view, TOCTree root) {
			super(view, root);
		}

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			final TOCTree tree = getItem(position);
			if (!tree.hasChildren()) {
				return false;
			}

			final ContextMenuDialog dialog = new ContextMenuDialog() {
				@Override
				protected String getTitle() {
					return tree.Text;
				}

				@Override
				protected void onItemClick(long itemId) {
					switch ((int)itemId) {
						case PROCESS_TREE_ITEM_ID:
							runTreeItem(tree);
							break;
						case READ_BOOK_ITEM_ID:
							openBookText(tree);
							break;
					}
				}
			};

			final ZLResource resource = ZLResource.resource("tocView");
			dialog.addItem(
				PROCESS_TREE_ITEM_ID,
				resource,
				isOpen(tree) ? "collapseTree" : "expandTree"
			);
			if (tree.Reference != null && tree.Reference != -1) {
				dialog.addItem(READ_BOOK_ITEM_ID, resource, "readText");
			}
			dialog.show(TOCActivity.this);
			return true;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final View view = (convertView != null) ? convertView :
				LayoutInflater.from(parent.getContext()).inflate(R.layout.toc_tree_item, parent, false);
			final TOCTree tree = getItem(position);
			view.setBackgroundColor(tree == mySelectedItem ? 0xff808080 : 0);
			setIcon(ViewUtil.findImageView(view, R.id.toc_tree_item_icon), tree);
			ViewUtil.setSubviewText(view, R.id.toc_tree_item_text, tree.Text);

			final Integer pageNo;
			if (tree.Reference != null && tree.Reference != -1) {
				final FBReaderApp fbreader = (FBReaderApp)FBReaderApp.Instance();
				pageNo = fbreader.BookTextView.pageNoFromParagraph(tree.Reference);
			} else {
				pageNo = null;
			}
			ViewUtil.setSubviewText(
				view, R.id.toc_tree_item_pageno, pageNo != null ? String.valueOf(pageNo) : ""
			);

			return view;
		}

		void openBookText(TOCTree tree) {
			if (tree.Reference != null && tree.Reference != -1) {
				final Intent intent = new Intent();
				intent.putExtra(MainActivity.TOCKey.REF, tree.Reference);
				setResult(RESULT_OK, intent);
				finish();
			}
		}

		@Override
		protected boolean runTreeItem(TOCTree tree) {
			if (super.runTreeItem(tree)) {
				return true;
			}
			openBookText(tree);
			return true;
		}
	}
}
