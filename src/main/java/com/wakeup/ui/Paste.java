package com.wakeup.ui;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

/** Typing missions only count if you actually type: no paste, no selection menu. */
public final class Paste {
    private Paste() {}

    public static void block(EditText e) {
        e.setLongClickable(false);
        e.setTextIsSelectable(false);
        ActionMode.Callback none = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }
        };
        e.setCustomSelectionActionModeCallback(none);
        e.setCustomInsertionActionModeCallback(none);
        e.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });
    }
}
