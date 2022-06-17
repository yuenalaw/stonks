package com.plcoding.stockmarketapp.data.repository

import com.plcoding.stockmarketapp.data.csv.CSVParser
import com.plcoding.stockmarketapp.data.local.StockDatabase
import com.plcoding.stockmarketapp.data.mapper.toCompanyListing
import com.plcoding.stockmarketapp.data.mapper.toCompanyListingEntity
import com.plcoding.stockmarketapp.data.remote.StockApi
import com.plcoding.stockmarketapp.domain.model.CompanyListing
import com.plcoding.stockmarketapp.domain.repository.StockRepository
import com.plcoding.stockmarketapp.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

//we want to depend on ABSTRACTIONS (like abstract classes or interfaces)
@Singleton //make sure only one instance in whole app
class StockRepositoryImpl @Inject constructor(
    val api: StockApi,
    val db: StockDatabase,
    val companyListingsParser: CSVParser<CompanyListing>
): StockRepository{

    private val dao = db.dao

    override suspend fun getCompanyListings(
        fetchFromRemote: Boolean,
        query: String
    ): Flow<Resource<List<CompanyListing>>> {
        return flow {
            emit(Resource.Loading(true))
            //after finishes loading...
            val localListings = dao.searchCompanyListing(query)
            emit(Resource.Success(
                data = localListings.map { it.toCompanyListing() } //can go straight to UI now!
            ))
            val isDbEmpty = localListings.isEmpty() && query.isBlank() //initial state
            val shouldJustLoadFromCache = !isDbEmpty && !fetchFromRemote
            if (shouldJustLoadFromCache) {
                emit(Resource.Loading(false)) //no more loading
                return@flow
            }
            val remoteListings = try {
                val response = api.getListings()
                companyListingsParser.parse(response.byteStream())
            } catch(e: IOException) {
                e.printStackTrace()
                emit(Resource.Error("Couldn't load data"))
                null //return null
            } catch (e: HttpException) {
                e.printStackTrace()
                emit(Resource.Error("Couldn't load data"))
                null
            }

            //insert into local cache
            remoteListings?.let { listings ->
                dao.clearCompanyListings()
                dao.insertCompanyListings(
                    listings.map { it.toCompanyListingEntity() }
                )
                emit(Resource.Success(
                    data = dao
                        .searchCompanyListing("")
                        .map { it.toCompanyListing() }))
                emit(Resource.Loading(false))
            }
        }
    }
}