/*
 * Copyright 2010 nookDevs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *              http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Written by Kevin Vajk
 */


package com.nookdevs.mines;

import android.content.Context;
import android.widget.TableLayout;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View;
import android.widget.TextView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
//import android.util.Log ;

/*
 * The MineField class uses this class to draw itself on both the eink screen
 * and the touchscreen.
 */


public class MineFieldViews {
	private MineField mineField;
	TableLayout minefieldtable; // eink
	ImageView littleandroid;
	public ListView touchscreen_listview;
	private TextView statusline;
	int rows;
	int cols;
	float eink_textsize;
	float ts_textsize;
	MinesPlayActivity activity;
	TextView eink_views[][];
	
	//  We need to know how much room we have, so we pick cells that
	//  will all fit on the screen:
	private final static int EINK_MINEFIELD_WIDTH = 510 ;
	private final static int TS_MINEFIELD_WIDTH = 408 ;
	private final static int EINK_MINEFIELD_HEIGHT = 685 ;

	int eink_cell_icon_unknown;
	int eink_cell_icon_cleared;
	int eink_cell_icon_flagged;
	int eink_cell_icon_explosion;
	int eink_cell_icon_mine;
	int eink_cell_icon_flagged_incorrectly;
	int ts_cell_icon_unknown;
	int ts_cell_icon_cleared;
	int ts_cell_icon_flagged;
	int ts_cell_icon_explosion;
	int ts_cell_icon_mine;
	int ts_cell_icon_flagged_incorrectly;
	
	MineFieldViews(MineField p) {
		mineField = p;
		activity = mineField.activity;
		minefieldtable = (TableLayout) activity
				.findViewById(R.id.minefieldtable);

		rows = mineField.rows;
		cols = mineField.cols;
		littleandroid = (ImageView) activity.findViewById(R.id.littleandroid);
		littleandroid.setImageResource(R.drawable.littleandroid);
		touchscreen_listview = (ListView) activity.findViewById(R.id.list);
		touchscreen_listview.setAdapter(new MineFieldListAdapter(activity));
		statusline = (TextView) activity.findViewById(R.id.einktext);
		pick_iconset();
		set_textsize();
		buildMineFieldViews();
		updateStatusText();
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//  The next few functions are called by our constructor:
	
	private void set_textsize() {
		float einkmaxy = EINK_MINEFIELD_HEIGHT / rows * .62f ;
		float einkmaxx = EINK_MINEFIELD_WIDTH / cols * .62f ;
		if ( einkmaxx < einkmaxy ) {
			eink_textsize = einkmaxx ;
		} else {
			eink_textsize = einkmaxy ;
		}
		ts_textsize = TS_MINEFIELD_WIDTH / cols * .62f ;
	} // set_textsize

	// eink:
	private void buildMineFieldViews() {
		LayoutInflater inflater = activity.getLayoutInflater();
		minefieldtable.removeAllViews();
		eink_views = new TextView[rows][cols];
		for (int r = 0; r < rows; r++) {
			// create table row
			TableRow tableRow = (TableRow) inflater.inflate(
					R.layout.eink_tablerow, null);
			for (int c = 0; c < cols; c++) {
				TextView cellview = (TextView) inflater.inflate(
						R.layout.eink_cell, null);
				cellview.setBackgroundResource(eink_cell_icon_unknown);
				cellview.setTextSize(eink_textsize);
				int dim;
				int einkmaxy = EINK_MINEFIELD_HEIGHT / rows ;
				int einkmaxx = EINK_MINEFIELD_WIDTH / cols ;
				if ( einkmaxx < einkmaxy ) {
					dim = einkmaxx ;
				} else {
					dim = einkmaxy ;
				}
				cellview.setMinimumHeight(dim);
				cellview.setMinimumWidth(dim);
				// add button to tablerow
				tableRow.addView(cellview);
				eink_views[r][c] = cellview;
			}
			// add tablerow to table:
			minefieldtable.addView(tableRow);
		}
	} // buildMineFieldViews

	
	//  I'm doing all this because android won't easily scale down a background image if
	//  it's too big, whereas it will scale it up if it's too small.  So we pick the
	//  biggest background icon that's smaller than our cell.
	//  I could have just used a small icon for everything, but an icon small enough
	//  for a 16x16 game looked blurry on an 8x8 game.
	//  I'm guessing there's a better way to accomplish all this, but I don't know it.
	private void pick_iconset() {
		int[] iconset62 = { R.drawable.cell_unknown_62,
				R.drawable.cell_cleared_62, R.drawable.cell_flagged_62,
				R.drawable.cell_explosion_62, R.drawable.cell_mine_62,
				R.drawable.cell_flagged_incorrectly_62, };
		int[] iconset51 = { R.drawable.cell_unknown_51,
				R.drawable.cell_cleared_51, R.drawable.cell_flagged_51,
				R.drawable.cell_explosion_51, R.drawable.cell_mine_51,
				R.drawable.cell_flagged_incorrectly_51, };
		int[] iconset42 = { R.drawable.cell_unknown_42,
				R.drawable.cell_cleared_42, R.drawable.cell_flagged_42,
				R.drawable.cell_explosion_42, R.drawable.cell_mine_42,
				R.drawable.cell_flagged_incorrectly_42, };
		int[] iconset34 = { R.drawable.cell_unknown_34,
				R.drawable.cell_cleared_34, R.drawable.cell_flagged_34,
				R.drawable.cell_explosion_34, R.drawable.cell_mine_34,
				R.drawable.cell_flagged_incorrectly_34, };
		int[] iconset25 = { R.drawable.cell_unknown_25,
				R.drawable.cell_cleared_25, R.drawable.cell_flagged_25,
				R.drawable.cell_explosion_25, R.drawable.cell_mine_25,
				R.drawable.cell_flagged_incorrectly_25, };
		int[] eink_iconset;
		int[] ts_iconset;

		//  EINK_MINEFIELD_WIDTH
		//  TS_MINEFIELD_WIDTH
		
		int einkmaxsize ;
		int einkmaxy = EINK_MINEFIELD_HEIGHT / rows ;
		int einkmaxx = EINK_MINEFIELD_WIDTH / cols ;
		if ( einkmaxx < einkmaxy ) {
			einkmaxsize = einkmaxx ;
		} else {
			einkmaxsize = einkmaxy ;
		}
		if ( einkmaxsize >= 62 ) {
			eink_iconset = iconset62;
		} else if ( einkmaxsize >= 51 ) {
			eink_iconset = iconset51;
		} else if ( einkmaxsize >= 42 ) {
			eink_iconset = iconset42;
		} else if ( einkmaxsize >= 34 ) {
			eink_iconset = iconset34;
		} else {
			eink_iconset = iconset25;
		}
		
		int tsmaxsize = TS_MINEFIELD_WIDTH / cols ;
		if ( tsmaxsize >= 62 ) {
			ts_iconset = iconset62;
		} else if ( tsmaxsize >= 51 ) {
			ts_iconset = iconset51;
		} else if ( tsmaxsize >= 42 ) {
			ts_iconset = iconset42;
		} else if ( tsmaxsize >= 34 ) {
			ts_iconset = iconset34;
		} else {
			ts_iconset = iconset25;
		}

		eink_cell_icon_unknown = eink_iconset[0];
		eink_cell_icon_cleared = eink_iconset[1];
		eink_cell_icon_flagged = eink_iconset[2];
		eink_cell_icon_explosion = eink_iconset[3];
		eink_cell_icon_mine = eink_iconset[4];
		eink_cell_icon_flagged_incorrectly = eink_iconset[5];

		ts_cell_icon_unknown = ts_iconset[0];
		ts_cell_icon_cleared = ts_iconset[1];
		ts_cell_icon_flagged = ts_iconset[2];
		ts_cell_icon_explosion = ts_iconset[3];
		ts_cell_icon_mine = ts_iconset[4];
		ts_cell_icon_flagged_incorrectly = ts_iconset[5];

	} // pick_iconset


	
	////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	public void cellHasChanged(int row, int col) {
		drawCell(row, col);
		updateStatusText();
	} // cellHasChanged
	
	// Draws a cell on the eInk screen, and notify the touchscreen adapter:
	private void drawCell(int row, int col) {
		TextView cellview = eink_views[row][col];
		if (mineField.cellIsCleared(row,col) ) {
			int num = mineField.getCellNum(row,col);
			if (num == 0) {
				cellview.setText("");
			} else {
				cellview.setText("" + num);
			}
		} else {
			cellview.setText("");
		}
		cellview.setBackgroundResource(getEinkCellIcon(row, col));
		((BaseAdapter) touchscreen_listview.getAdapter())
				.notifyDataSetChanged();
	} // drawCell

	public void gameLost() {
		littleandroid.setImageResource(R.drawable.littleandroid_fallen);
		// re-draw everything, to reveal mines, etc:
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				drawCell(r, c);
			}
		}
		updateStatusText();
	} // gameLost
	
	public void gameWon() {
		littleandroid.setImageResource(R.drawable.littleandroid_victory);
		updateStatusText();
	} // gameWon


	public void removeAll() {
		minefieldtable.removeAllViews();
	} // removeAll

	
	private int[] getCellIcons(int r, int c) {
		int ts_icon;
		int eink_icon;
		int[] icons = new int[2];
		if ( mineField.cellIsCleared(r,c) ) {
			eink_icon = eink_cell_icon_cleared;
			ts_icon = ts_cell_icon_cleared;
		} else {
			if (mineField.minefield_state == MineField.MINEFIELDSTATE_LOST ) {
				//  Game over, reveal all:
				if ( mineField.cellHasExplosion(r,c) ) {
					eink_icon = eink_cell_icon_explosion;
					ts_icon = ts_cell_icon_explosion;
				} else if ( mineField.cellHasAMine(r,c) ) {
					if ( mineField.cellIsFlagged(r,c) ) {
						eink_icon = eink_cell_icon_flagged;
						ts_icon = ts_cell_icon_flagged;
					} else {
						eink_icon = eink_cell_icon_mine;
						ts_icon = ts_cell_icon_mine;
					}
				} else if ( mineField.cellIsFlagged(r,c) ) {
					eink_icon = eink_cell_icon_flagged_incorrectly;
					ts_icon = ts_cell_icon_flagged_incorrectly;
				} else {
					eink_icon = eink_cell_icon_unknown;
					ts_icon = ts_cell_icon_unknown;
				}

			} else {
				//  Normal play:
				if ( mineField.cellIsFlagged(r,c) ) {
					eink_icon = eink_cell_icon_flagged;
					ts_icon = ts_cell_icon_flagged;
				} else {
					eink_icon = eink_cell_icon_unknown;
					ts_icon = ts_cell_icon_unknown;
				}
			}
		}
		icons[0] = eink_icon;
		icons[1] = ts_icon;
		return (icons);
	} // getCellIcons

	private int getTSCellIcon(int r, int c) {
		int icons[] = getCellIcons(r, c);
		return (icons[1]);
	} // getTSCellIcon

	private int getEinkCellIcon(int r, int c) {
		int icons[] = getCellIcons(r, c);
		return (icons[0]);
	} // getEinkCellIcon

	//  A class we'll use as a tag for the touchscreen cell buttons
	private class CellButtonArgs {
		int row;
		int col;
		MineField mineField;
		CellButtonArgs(int r, int c, MineField m) {
			row = r;
			col = c;
			mineField = m;
		}
	} // CellButtonArgs

	
	//  Used by the adapter to get a row of cells on the touchscreen
	View getTSView(int position, View convertView) {
		LinearLayout layout;
		LayoutInflater inflater = activity.getLayoutInflater();
		if (convertView != null) {
			layout = (LinearLayout) convertView;
		} else {
			layout = (LinearLayout) inflater.inflate(R.layout.ts_row, null);
			for (int c = 0; c < cols; c++) {
				Button b = (Button) inflater.inflate(R.layout.ts_cellbutton, null);
				layout.addView(b, c);
			}

		}
		
		int r = position;
		for (int c = 0; c < cols; c++) {
			Button b = (Button) layout.getChildAt(c);
			if ( mineField.cellIsCleared(r,c) ) {
				int num = mineField.getCellNum(r,c) ;
				if ( num == 0) {
					b.setText("");
				} else {
					b.setText("" + num);
				}
			} else {
				b.setText("");
			}
			b.setTextSize( ts_textsize );
			b.setBackgroundResource(getTSCellIcon(r, c));
			CellButtonArgs a = new CellButtonArgs(r, c, mineField);
			b.setTag(a);
			b.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					CellButtonArgs a = (CellButtonArgs) v.getTag();
					// a.mineField.clearCell( a.row, a.col );
					a.mineField.activity.clickedCell(a.row, a.col);
				}
			});
			b.setLongClickable(true);
			b.setOnLongClickListener(new OnLongClickListener() {
				public boolean onLongClick(View v) {
					CellButtonArgs a = (CellButtonArgs) v.getTag();
					a.mineField.activity.longClickedCell(a.row, a.col);
					return true;
				}
			});
			int dim = TS_MINEFIELD_WIDTH / cols;
			b.setMinimumHeight(dim);
			b.setMinimumWidth(dim);
		}

		return ((View) layout);
	} // getTSView
	
	//  Updates the line of text on the eink screen just below the minefield
	private void updateStatusText() {
		if ( mineField.minefield_state == MineField.MINEFIELDSTATE_PLAYING ) {
			statusline.setText( activity.getText(R.string.flags) + ": " + mineField.planted_flags + "/" + mineField.num_mines );
		} else if ( mineField.minefield_state == MineField.MINEFIELDSTATE_WON ) {
			statusline.setText("");  // TODO: elapsed time
		} else if ( mineField.minefield_state == MineField.MINEFIELDSTATE_LOST ) {
			statusline.setText("");
		} else {
			statusline.setText("");
		}
	} // updateText
	
	// ///////////////////////////////////////////////////////////////////////////////////

	private class MineFieldListAdapter extends BaseAdapter {
		// Context mContext ;

		public MineFieldListAdapter(Context c) {
			// mContext = c ;
		}

		public int getCount() {
			if (mineField == null) {
				return (0);
			}
			return mineField.rows;
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			return (mineField.mineFieldViews.getTSView(position, convertView));
		}
	}

}
