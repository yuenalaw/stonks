package com.plcoding.stockmarketapp.domain.repository

import com.plcoding.stockmarketapp.domain.model.CompanyListing
import com.plcoding.stockmarketapp.util.Resource
import kotlinx.coroutines.flow.Flow //flow emits multiple values over a period of time

//repository deals with the caching.
//when we put this into our new models that access our repository, view model should only be able to
//access classes from the domain layer, so we use an interface here.
//the implementation is done in the data layer
//domain layer is innermost layer, so presentation layer can access it


//domain layer SHOULD NOT ACCESS THE DATA LAYER
interface StockRepository {

    suspend fun getCompanyListings(
        fetchFromRemote: Boolean, //otherwise straight from cache
        query: String
    ): Flow<Resource<List<CompanyListing>>>

}