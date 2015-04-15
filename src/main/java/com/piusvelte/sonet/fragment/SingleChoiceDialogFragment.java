package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

/**
 * Created by bemmanuel on 4/11/15.
 */
public class SingleChoiceDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private static final String ARG_REQUEST_CODE = "request_code";
    private static final String ARG_ITEMS = "items";
    private static final String ARG_WHICH = "which";
    private static final String ARG_TITLE = "title";

    public static SingleChoiceDialogFragment newInstance(@NonNull CharSequence[] items, int which, @NonNull String title, int requestCode) {
        Bundle args = new Bundle();
        args.putInt(ARG_REQUEST_CODE, requestCode);
        args.putCharSequenceArray(ARG_ITEMS, items);
        args.putInt(ARG_WHICH, which);
        args.putString(ARG_TITLE, title);

        SingleChoiceDialogFragment chooseAccountDialogFragment = new SingleChoiceDialogFragment();
        chooseAccountDialogFragment.setArguments(args);
        return chooseAccountDialogFragment;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        return new AlertDialog.Builder(getActivity())
                .setTitle(args.getString(ARG_TITLE))
                .setSingleChoiceItems(args.getCharSequenceArray(ARG_ITEMS), args.getInt(ARG_WHICH), this)
                .create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        int requestCode = getArguments().getInt(ARG_REQUEST_CODE);
        Fragment target = getTargetFragment();

        if (target != null) {
            target.onActivityResult(requestCode, Activity.RESULT_CANCELED, null);
        } else {
            Fragment parent = getParentFragment();

            if (parent != null) {
                parent.onActivityResult(requestCode, Activity.RESULT_CANCELED, null);
            } else {
                Activity activity = getActivity();

                if (activity instanceof OnDialogFragmentFinishListener) {
                    ((OnDialogFragmentFinishListener) activity).onDialogFragmentResult(requestCode, Activity.RESULT_CANCELED, null);
                }
            }
        }

        dismiss();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Bundle args = getArguments();
        args.putInt(ARG_WHICH, which);
        int requestCode = args.getInt(ARG_REQUEST_CODE);

        Intent intent = new Intent()
                .putExtras(args);
        Fragment target = getTargetFragment();

        if (target != null) {
            target.onActivityResult(requestCode, Activity.RESULT_OK, intent);
        } else {
            Fragment parent = getParentFragment();

            if (parent != null) {
                parent.onActivityResult(requestCode, Activity.RESULT_OK, intent);
            } else {
                Activity activity = getActivity();

                if (activity instanceof OnDialogFragmentFinishListener) {
                    ((OnDialogFragmentFinishListener) activity).onDialogFragmentResult(requestCode, Activity.RESULT_OK, intent);
                }
            }
        }

        dismiss();
    }

    public static int getWhich(@Nullable Intent intent, int defaultValue) {
        if (intent == null) {
            return defaultValue;
        }

        return intent.getIntExtra(ARG_WHICH, defaultValue);
    }
}
