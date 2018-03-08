package com.demo.retrofitrxjava.network;

import com.demo.retrofitrxjava.model.Person;


import io.reactivex.Observable;
import retrofit2.http.GET;

/**
 * Created by PCPV on 03/08/2018.
 */

public interface IGetPerson {
    @GET(".")
    Observable<Person> getProfile();
}
