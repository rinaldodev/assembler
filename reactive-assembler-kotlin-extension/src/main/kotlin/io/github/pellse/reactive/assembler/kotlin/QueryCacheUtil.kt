package io.github.pellse.reactive.assembler.kotlin

import io.github.pellse.reactive.assembler.Mapper
import io.github.pellse.reactive.assembler.QueryCache
import io.github.pellse.reactive.assembler.QueryCache.cached
import java.time.Duration

fun <ID, R> Mapper<ID, R>.cached(): Mapper<ID, R>  = cached(this)
fun <ID, R> Mapper<ID, R>.cached(cache: QueryCache<ID, R>): Mapper<ID, R> = cached(this, cache)
fun <ID, R> Mapper<ID, R>.cached(ttl: Duration): Mapper<ID, R>  = cached(this, ttl)
fun <ID, R> Mapper<ID, R>.cached(cache: QueryCache<ID, R>, ttl: Duration): Mapper<ID, R>  = cached(this, cache, ttl)
