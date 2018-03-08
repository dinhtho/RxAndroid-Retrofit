package com.demo.retrofitrxjava;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import com.demo.retrofitrxjava.model.Android;
import com.demo.retrofitrxjava.model.AndroidsAndPerson;
import com.demo.retrofitrxjava.model.Person;
import com.demo.retrofitrxjava.network.IGetPerson;
import com.demo.retrofitrxjava.network.IRequestInterface;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private RecyclerView mRecyclerView;

    private CompositeDisposable mCompositeDisposable;

    private DataAdapter mAdapter;

    private ArrayList<Android> mAndroidArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCompositeDisposable = new CompositeDisposable();
        initRecyclerView();
        loadJSON();
    }

    private void initRecyclerView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        mRecyclerView.setLayoutManager(layoutManager);
    }

    private void loadJSON() {

        IRequestInterface requestInterface = new Retrofit.Builder()
                .baseUrl("https://api.learn2crack.com/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(IRequestInterface.class);

        IGetPerson getPerson = new Retrofit.Builder()
                .baseUrl("http://uinames.com/api/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(IGetPerson.class);

        callOneApi(requestInterface);
        callOneApiWithPureCode(requestInterface);
        callChainApis(requestInterface,getPerson);

        callParallelApisWithZip(requestInterface, getPerson);
        callParallelApisWithMerge(requestInterface, getPerson);

    }

    private void handleResponse(List<Android> androidList) {
        mAndroidArrayList = new ArrayList<>(androidList);
        mAdapter = new DataAdapter(mAndroidArrayList);
        mRecyclerView.setAdapter(mAdapter);
    }


    private void handleError(Throwable error) {

        Toast.makeText(this, "Error " + error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
    }

    private void handleSuccess() {
        Toast.makeText(this, "Get data success! ", Toast.LENGTH_SHORT).show();
    }

    private void callOneApiWithPureCode(IRequestInterface requestInterface) {
        Disposable disposable = requestInterface.register()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<List<Android>>() {
                               @Override
                               public void accept(List<Android> androids) throws Exception {
                                   Log.i(TAG, "callOneApiWithPureCode: " + androids.size());
                               }
                           }, new Consumer<Throwable>() {
                               @Override
                               public void accept(Throwable throwable) throws Exception {
                                   Log.i(TAG, "callOneApiWithPureCode: error");

                               }
                           }, new Action() {
                               @Override
                               public void run() throws Exception {
                                   Log.i(TAG, "callOneApiWithPureCode: complete");
                               }
                           }
                );

        mCompositeDisposable.add(disposable);
    }

    private void callChainApis(IRequestInterface requestInterface, IGetPerson getPerson) {
        Disposable disposable = getPerson.getProfile()
                .flatMap(person -> {
                    Log.i(TAG, "callChainApis: " + person.getName());
                    return requestInterface.register();
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(this::handleResponse, this::handleError, this::handleSuccess);

        mCompositeDisposable.add(disposable);
    }

    /*
    *  The result will return in one time when all requests finish
    * */
    private void callParallelApisWithZip(IRequestInterface requestInterface, IGetPerson getPerson) {
        Disposable disposable = Observable.zip(requestInterface.register(), getPerson.getProfile(), new BiFunction<List<Android>, Person, AndroidsAndPerson>() {
            @Override
            public AndroidsAndPerson apply(List<Android> androids, Person person) throws Exception {
                Log.i(TAG, "apply: " + person.getName());
                return new AndroidsAndPerson(androids, person);
            }
        })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<AndroidsAndPerson>() {
                               @Override
                               public void accept(AndroidsAndPerson androidsAndPerson) throws Exception {
                                   Log.i(TAG, "accept: " + androidsAndPerson.getPerson().getName());
                                   Log.i(TAG, "accept: " + androidsAndPerson.getAndroids().size());
                               }
                           },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {

                            }
                        },
                        new Action() {
                            @Override
                            public void run() throws Exception {

                            }
                        });
        mCompositeDisposable.add(disposable);
    }

    /*
       *  The result will return in many times when each request finish
       * */
    private void callParallelApisWithMerge(IRequestInterface requestInterface, IGetPerson getPerson) {

        Disposable disposable = Observable.merge(requestInterface.register(), getPerson.getProfile())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object object) throws Exception {
                        if (object instanceof List) {
                            Log.i(TAG, "accept: " + ((List) object).size());
                        } else if (object instanceof Person) {
                            Log.i(TAG, "accept: " + ((Person) object).getName());
                        }
                    }
                });
        mCompositeDisposable.add(disposable);
    }


    private void callOneApi(IRequestInterface requestInterface) {
        Disposable disposable = requestInterface.register()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(this::handleResponse, this::handleError, this::handleSuccess);

        mCompositeDisposable.add(disposable);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mCompositeDisposable.clear();
    }

}
