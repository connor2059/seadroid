package com.seafile.seadroid2.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.seafile.seadroid2.R;
import com.seafile.seadroid2.SeafConnection;
import com.seafile.seadroid2.SeafException;
import com.seafile.seadroid2.account.Account;
import com.seafile.seadroid2.data.DataManager;
import com.seafile.seadroid2.data.SeafLink;

import java.util.ArrayList;

class GetShareLinkTask extends TaskDialog.Task {
    Account account;
    String repoID;
    String path;
    String password;
    SeafConnection conn;
    String link;
    String days;

    public GetShareLinkTask(String repoID, String path, String password, SeafConnection conn, String days,
                            Account account) {
        this.repoID = repoID;
        this.path = path;
        this.password = password;
        this.conn = conn;
        this.days = days;
        this.account = account;
    }

    @Override
    protected void runTask() {
        try {
            // If you has  Shared links to delete Shared links
            DataManager dataManager = new DataManager(account);
            ArrayList<SeafLink> shareLinks = dataManager.getShareLink(repoID, path);
            for (SeafLink shareLink : shareLinks) {
                //delete link
                dataManager.deleteShareLink(shareLink.getToken());
            }
            //create new link
            link = conn.getShareLink(repoID, path, password, days);
        } catch (SeafException e) {
            setTaskException(e);
        }
    }

    public String getResult() {
        return link;
    }
}

public class GetShareLinkDialog extends TaskDialog implements CompoundButton.OnCheckedChangeListener {
    private static final String STATE_TASK_REPO_NAME = "state_task_repo_name_share";
    private static final String STATE_TASK_REPO_ID = "state_task_repo_id_share";
    private String repoID;
    private String path;
    private boolean isEncrypt = false;
    private SeafConnection conn;
    private EditText passwordText;
    private String repoName;
    private EditText days;
    private CheckBox cbExpiration;
    private Account account;

    public void init(String repoID, String path, boolean isEncrypt, Account account) {
        this.repoID = repoID;
        this.path = path;
        this.isEncrypt = isEncrypt;
        this.account = account;
        this.conn = new SeafConnection(account);
    }

    @Override
    protected View createDialogContentView(LayoutInflater inflater, Bundle savedInstanceState) {
        View view = null;
        if (isEncrypt) {
            view = inflater.inflate(R.layout.dialog_share_password, null);
            passwordText = (EditText) view.findViewById(R.id.password);
            days = (EditText) view.findViewById(R.id.days);
            cbExpiration = (CheckBox) view.findViewById(R.id.add_expiration);
            cbExpiration.setOnCheckedChangeListener(this);

            if (savedInstanceState != null) {
                repoName = savedInstanceState.getString(STATE_TASK_REPO_NAME);
                repoID = savedInstanceState.getString(STATE_TASK_REPO_ID);
            }
        }
        return view;
    }


    @Override
    protected void onDialogCreated(Dialog dialog) {
        if (isEncrypt) {
            dialog.setTitle(getString(R.string.share_input_password));
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        } else {
            dialog.setTitle(getActivity().getString(R.string.generating_link));
        }
    }

    @Override
    protected void onSaveDialogContentState(Bundle outState) {
        outState.putString(STATE_TASK_REPO_NAME, repoName);
        outState.putString(STATE_TASK_REPO_ID, repoID);
    }

    @Override
    protected void onValidateUserInput() throws Exception {
        String password = passwordText.getText().toString().trim();
        String day = days.getText().toString().trim();

        if (password.length() == 0) {
            String err = getActivity().getResources().getString(R.string.password_empty);
            throw new Exception(err);
        }

        if (password.length() < getResources().getInteger(R.integer.minimum_password_length)) {
            throw new Exception(getResources().getString(R.string.err_passwd_too_short));
        }

        if (cbExpiration.isChecked() && day.length() == 0) {
            String err = getActivity().getResources().getString(R.string.input_auto_expiration);
            throw new Exception(err);
        }
    }

    @Override
    protected void disableInput() {
        super.disableInput();
        if (isEncrypt) {
            passwordText.setEnabled(false);
        }
    }

    @Override
    protected void enableInput() {
        super.enableInput();
        if (isEncrypt) {
            passwordText.setEnabled(true);
        }
    }


    @Override
    protected boolean executeTaskImmediately() {
        return !isEncrypt;
    }

    @Override
    protected GetShareLinkTask prepareTask() {
        String password = null;
        String days = null;
        if (isEncrypt) {
            password = passwordText.getText().toString().trim();
        }
        if (cbExpiration.isChecked()) {
            days = this.days.getText().toString().trim();
        }
        GetShareLinkTask task = new GetShareLinkTask(repoID, path, password, conn, days, account);
        return task;
    }

    public String getLink() {
        if (getTask() != null) {
            GetShareLinkTask task = (GetShareLinkTask) getTask();
            return task.getResult();
        }

        return null;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (errorIsVisible()) {
            hideError();
        }
        if (isChecked) {
            days.setVisibility(View.VISIBLE);
        } else {
            days.setVisibility(View.GONE);

        }
    }
}