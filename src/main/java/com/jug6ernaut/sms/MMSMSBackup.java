package com.jug6ernaut.sms;

import android.app.Activity;
import android.app.ProgressDialog;
import eu.chainfire.libsuperuser.Debug;
import eu.chainfire.libsuperuser.Shell;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.Arrays;
import java.util.List;

/**
 * Created by williamwebb on 3/1/14.
 */
public class MMSMSBackup {

    private static final  String TELEPHONY_FOLDER = "/data/data/com.android.providers.telephony/";

    private static final String commandCD = "cd %s";
    private static final String commandCAT = "cat %s > %s";
    private static final String commandTar = "tar -zcvf %s *";
    private static final String commandUnTar = "tar -zxvf %s";
    private static final String commandRM = "rm %s";
    private static final String commandViewArchive = "tar -tf %s %s";

    public static Observable<List<String>> restore(final Activity activity, final String savePath,final String fileName){
        return Observable.create(new Observable.OnSubscribe<List<String>>() {
            @Override
            public void call(final Subscriber<? super List<String>> observer) {
                final ProgressDialog dialog = launchRingDialog(activity, "restoring " + fileName + "...");
                final Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            getShellListener(new ShellObserver(){
                                @Override
                                public void onError(Throwable e) {

                                }

                                @Override
                                public void onNext(String args) {
                                    dialog.setMessage(args);
                                }
                            });

                            observer.onNext(Shell.SU.run(Arrays.asList(
                                    String.format(commandCD, savePath),
                                    String.format(commandCAT, savePath + fileName, TELEPHONY_FOLDER + fileName),
                                    String.format(commandCD, TELEPHONY_FOLDER),
                                    String.format(commandUnTar, fileName),
                                    String.format(commandRM, fileName))));
                            observer.onCompleted();
                            dialog.dismiss();
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }
                };
                thread.start();
            }
        }).observeOn(Schedulers.io())
          .subscribeOn(AndroidSchedulers.mainThread());
    }

    public static Observable<List<String>> backup(final Activity activity, final String savePath, final String fileName){
        return Observable.create(new Observable.OnSubscribe<List<String>>() {
            @Override
            public void call(final Subscriber<? super List<String>> observer) {
                final ProgressDialog dialog = launchRingDialog(activity, "backing up " + fileName + "...");
                final Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            getShellListener(new ShellObserver(){
                                @Override
                                public void onError(Throwable e) {

                                }

                                @Override
                                public void onNext(String args) {
                                    dialog.setMessage(args);
                                }
                            });

                                    observer.onNext(Shell.SU.run(Arrays.asList(
                                            String.format(commandCD, TELEPHONY_FOLDER),
                                            String.format(commandTar, fileName),
                                            String.format(commandCAT, fileName, savePath + fileName),
                                            String.format(commandRM, fileName))));
                            observer.onCompleted();
                            dialog.dismiss();
                        } catch (Exception e) {
                            observer.onError(e);
                            dialog.dismiss();
                        }
                    }
                };
                thread.start();
            }
        }).observeOn(Schedulers.io())
          .subscribeOn(AndroidSchedulers.mainThread());
    }

    public static Observable<Boolean> validate(final String savePath, final String fileName){
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(final Subscriber<? super Boolean> observer) {
                final Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            List<String> files = Shell.SU.run(Arrays.asList(
                                    String.format(commandViewArchive, savePath + fileName, "databases")));

                            boolean hasDBs = (
                                    files.contains("databases/mmssms.db") &&
                                            files.contains("databases/mmssms.db-journal")
                            );
                            observer.onNext(hasDBs);
                            observer.onCompleted();
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }
                };
                thread.start();
            }
        }).observeOn(Schedulers.io())
          .subscribeOn(AndroidSchedulers.mainThread());
    }

    public static Observable delete(final String savePath, final String fileName){
        return Observable.create(new Observable.OnSubscribe<List<String>>() {
            @Override
            public void call(final Subscriber<? super List<String>> observer) {
                final Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            observer.onNext(Shell.SU.run(Arrays.asList(
                                    String.format(commandRM,  savePath + fileName))));
                            observer.onCompleted();
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }
                };
                thread.start();
            }
        }).observeOn(Schedulers.io())
          .subscribeOn(AndroidSchedulers.mainThread());
    }

    public static Observable<Boolean> haveSU(final Activity activity){
        return Observable.create(new Observable.OnSubscribe<Boolean>() {

            @Override
            public void call(final Subscriber<? super Boolean> subscriber) {
                final ProgressDialog dialog = launchRingDialog(activity, "checking for SuperUser..");
                final Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            subscriber.onNext(Shell.SU.available());
                            subscriber.onCompleted();
                            dialog.dismiss();
                        } catch (Exception e) {
                            subscriber.onNext(false);
                            subscriber.onError(e);
                        }
                    }
                };
                thread.start();
            }
        }).observeOn(Schedulers.io())
          .subscribeOn(AndroidSchedulers.mainThread());
    }

    public static ProgressDialog launchRingDialog(final Activity activity,final String message) {
        final ProgressDialog[] ringProgressDialog = new ProgressDialog[1];
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ringProgressDialog[0] = ProgressDialog.show(activity, "Please wait ...", message, true);
                ringProgressDialog[0].setCancelable(false);
            }
        });

        return ringProgressDialog[0];
    }

    static PublishSubject<String> loginEventPublisher = PublishSubject.create();

    public static Subscription getShellListener(Observer<String> listener){
        Subscription subscription = loginEventPublisher
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(listener);
        return subscription;

    }

    private static abstract class ShellObserver implements Observer<String> {

        protected Subscription subscription;

        @Override
        public void onCompleted() {
            subscription.unsubscribe();
        }

    }

    static {
        Debug.setLogTypeEnabled(Debug.LOG_NONE, true);
        Debug.setLogTypeEnabled(Debug.LOG_OUTPUT, true);
        Debug.setOnLogListener(new Debug.OnLogListener() {
            @Override
            public void onLog(int i, final String s, final String s2) {
                System.out.println(i + " : " + s + " : " + s2);
                loginEventPublisher.onNext(s2);
            }
        });
    }

}
