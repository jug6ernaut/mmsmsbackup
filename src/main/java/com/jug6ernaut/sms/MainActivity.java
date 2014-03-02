package com.jug6ernaut.sms;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.jug6ernaut.sms.EULA.EULA;
import com.michaelflisar.licenses.Helper;
import com.michaelflisar.licenses.dialog.LicensesDialog;
import com.michaelflisar.licenses.licenses.BaseLicenseEntry;
import com.michaelflisar.licenses.licenses.Licenses;
import com.michaelflisar.universalloader.ULActivity;
import de.timroes.android.listview.EnhancedListView;
import rx.functions.Action1;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by williamwebb on 2/26/14.
 */
public class MainActivity extends ULActivity implements AdapterView.OnItemClickListener {

    private String BACKUP_FOLDER;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    @InjectView(R.id.buttonBackup)  Button backupButton;
    @InjectView(R.id.listView)      EnhancedListView listView;
    ArrayAdapter<String> adapter;
    List<String> files = new ArrayList<String>();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        ButterKnife.inject(this);
        EULA eula = new EULA(this);
        eula.show(false,true,new EULA.OnOkListener() {
            @Override
            public void onOk() {
                MMSMSBackup.haveSU(MainActivity.this).subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean found) {
                        System.out.println("Found: " + found);
                        if(!found)showError();
                    }
                });
            }
        });
        if(eula.isAccepted()){
            MMSMSBackup.haveSU(MainActivity.this).subscribe(new Action1<Boolean>() {
                @Override
                public void call(Boolean found) {
                    System.out.println("Found: " + found);
                    if(!found)showError();
                }
            });
        }
        loadLicenses();

        BACKUP_FOLDER = this.getExternalFilesDir("").toString() + File.separator;

        adapter = new ArrayAdapter<String>(this,R.layout.list_item,R.id.text,files);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        listView.setSwipingLayout(R.id.swiping_layout);
        listView.setDismissCallback(new de.timroes.android.listview.EnhancedListView.OnDismissCallback() {

            @Override
            public EnhancedListView.Undoable onDismiss(EnhancedListView listView, final int position) {

                final String item = files.remove(position);
                MMSMSBackup.delete(BACKUP_FOLDER, item).subscribe();
                adapter.notifyDataSetInvalidated();
                return null;
            }
        });
        listView.setUndoStyle(EnhancedListView.UndoStyle.SINGLE_POPUP);
        listView.enableSwipeToDismiss();
        listView.setSwipeDirection(EnhancedListView.SwipeDirection.BOTH);
        reload();
    }

    public void reload(){
        ArrayList<String> files = AnalysisDir.getFiles(BACKUP_FOLDER);
        final List<String> names = new ArrayList<String>(files.size());
        for(String file : files){
            names.add(new File(file).getName());
        }

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.files.clear();
                MainActivity.this.files.addAll(names);
                MainActivity.this.adapter.notifyDataSetInvalidated();
                MainActivity.this.listView.invalidate();
                MainActivity.this.listView.invalidateViews();
            }
        });

    }

    @OnClick(R.id.buttonBackup)
    public void backupOnClick(){
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle("File Name");
        final EditText nameBox = new EditText(this);
        nameBox.setText(dateFormat.format(new Date()));
        dialog.setView(nameBox);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE,"Backup",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                MMSMSBackup.backup(MainActivity.this, BACKUP_FOLDER, nameBox.getText().toString() + ".tgz").subscribe( new Action1<List<String>>() {
                    @Override
                    public void call(List<String> strings) {
                        reload();
                    }
                });
            }
        });
        dialog.show();
    }

    MenuCreator menuHandler;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menuHandler = new MenuCreator(menu);

        MenuCreator.TopLevelMenu menuButton = menuHandler.addTopLevelMenu(1, "");
        menuButton.addItem("About",0, aboutOperation);
        menuButton.addItem("License", 1, licenseOperation);
        return super.onCreateOptionsMenu(menu);
    }

    MenuCreator.MenuOperation licenseOperation = new MenuCreator.MenuOperation() {
        @Override
        public boolean operation(MenuItem item) {
            LicensesDialog dialog = new LicensesDialog(list);
            dialog.show(getSupportFragmentManager(), this.getClass().getName());
            return false;
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem menu){
        return menuHandler.handleMenuPress(menu);
    }

    private MenuCreator.MenuOperation aboutOperation = new MenuCreator.MenuOperation() {
        @Override
        public boolean operation(MenuItem item) {
            new EULA(MainActivity.this).show(true,false);
            return false;
        }
    };

    private AlertDialog restoreDialog;
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
        final String fileName = files.get(i);
        if(restoreDialog != null && restoreDialog.isShowing())return;

        restoreDialog = new AlertDialog.Builder(this).create();
        restoreDialog.setTitle("Restore");
        restoreDialog.setMessage("Restore " + fileName + "?");
        restoreDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Restore", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, final int i) {
                MMSMSBackup.validate(BACKUP_FOLDER, fileName).subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean valid) {
                        if (valid) {
                            MMSMSBackup.restore(MainActivity.this, BACKUP_FOLDER, fileName).subscribe();
                        } else {
                            Toast.makeText(MainActivity.this, "Invalid Backup File.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
        restoreDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        restoreDialog.show();

    }

    private List<BaseLicenseEntry> list = new ArrayList<BaseLicenseEntry>();
    private void loadLicenses(){
        list.add(Licenses.createGitLicense("jug6ernaut/mmsmsbackup", "LICENSE"));
        list.add(Licenses.createGitLicense("Chainfire/libsuperuser", "LICENSE"));
        list.add(Licenses.createGitLicense("JakeWharton/butterknife"));
        list.add(Licenses.createGitLicense("Netflix/RxJava", "LICENSE"));
        list.add(Licenses.createGitLicense("timroes/EnhancedListView", "LICENSE"));

        list.add(Licenses.createGitLicense("MichaelFlisar/MessageBar"));
        list.add(Licenses.createGitLicense("MichaelFlisar/UniversalLoader"));
        list.add(Licenses.createGitLicense("MichaelFlisar/LicensesDialog"));

        list.add(new BaseLicenseEntry("Android Support Library (v4)", "master", "Google", "", Licenses.LICENSE_APACHE_V2) {
            @Override public void doLoad() { this.license.setText(Helper.readFile(Licenses.LICENSE_APACHE_V2.getUrl())); }
        });
    }

    private void showError(){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).create();
                dialog.setTitle("Error");
                dialog.setMessage("Root was not found, will now close.");
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        MainActivity.this.finish();
                    }
                });
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        MainActivity.this.finish();
                    }
                });
                dialog.setButton(DialogInterface.BUTTON_POSITIVE,"Ok",new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        MainActivity.this.finish();
                    }
                });
                dialog.show();
            }
        });
    }

}