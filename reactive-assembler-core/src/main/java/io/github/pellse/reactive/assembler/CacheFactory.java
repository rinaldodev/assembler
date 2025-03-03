package io.github.pellse.reactive.assembler;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.github.pellse.reactive.assembler.Cache.cache;
import static io.github.pellse.reactive.assembler.RuleMapperSource.call;
import static io.github.pellse.util.collection.CollectionUtil.*;
import static reactor.core.publisher.Flux.fromStream;

@FunctionalInterface
public interface CacheFactory<ID, R> {

    Cache<ID, R> create(Function<Iterable<? extends ID>, Mono<Map<ID, Collection<R>>>> fetchFunction);

    static <ID, IDC extends Collection<ID>, R, RRC> RuleMapperSource<ID, IDC, R, RRC> cached(Function<IDC, Publisher<R>> queryFunction) {
        return cached(call(queryFunction));
    }

    static <ID, IDC extends Collection<ID>, R, RRC> RuleMapperSource<ID, IDC, R, RRC> cached(RuleMapperSource<ID, IDC, R, RRC> ruleMapperSource) {
        return cached(ruleMapperSource, cache());
    }

    static <ID, IDC extends Collection<ID>, R, RRC> RuleMapperSource<ID, IDC, R, RRC> cached(
            Function<IDC, Publisher<R>> queryFunction,
            Supplier<Map<ID, Collection<R>>> mapSupplier) {
        return cached(call(queryFunction), mapSupplier);
    }

    static <ID, IDC extends Collection<ID>, R, RRC> RuleMapperSource<ID, IDC, R, RRC> cached(
            RuleMapperSource<ID, IDC, R, RRC> ruleMapperSource,
            Supplier<Map<ID, Collection<R>>> mapSupplier) {
        return cached(ruleMapperSource, cache(mapSupplier));
    }

    static <ID, IDC extends Collection<ID>, R, RRC> RuleMapperSource<ID, IDC, R, RRC> cached(
            Function<IDC, Publisher<R>> queryFunction,
            Map<ID, Collection<R>> delegateMap) {
        return cached(call(queryFunction), delegateMap);
    }

    static <ID, IDC extends Collection<ID>, R, RRC> RuleMapperSource<ID, IDC, R, RRC> cached(
            RuleMapperSource<ID, IDC, R, RRC> ruleMapperSource,
            Map<ID, Collection<R>> delegateMap) {
        return cached(ruleMapperSource, cache(delegateMap));
    }

    static <ID, IDC extends Collection<ID>, R, RRC> RuleMapperSource<ID, IDC, R, RRC> cached(
            Function<IDC, Publisher<R>> queryFunction,
            CacheFactory<ID, R> cache) {
        return cached(call(queryFunction), cache);
    }

    static <ID, IDC extends Collection<ID>, R, RRC> RuleMapperSource<ID, IDC, R, RRC> cached(
            RuleMapperSource<ID, IDC, R, RRC> ruleMapperSource,
            CacheFactory<ID, R> cacheFactory) {

        return ruleContext -> {
            Function<Iterable<? extends ID>, Mono<Map<ID, Collection<R>>>> fetchFunction =
                    ids -> Flux.from(ruleMapperSource.apply(ruleContext).apply(translate(ids, ruleContext.idCollectionFactory())))
                            .collectMultimap(ruleContext.idExtractor())
                            .map(queryResultsMap -> buildCacheFragment(ids, queryResultsMap));

            final var cache = cacheFactory.create(fetchFunction);
            return ids -> cache.getAll(ids)
                    .flatMapMany(map -> fromStream(map.values().stream().flatMap(Collection::stream)));
        };
    }

    private static <ID, R> Map<ID, Collection<R>> buildCacheFragment(Iterable<? extends ID> entityIds, Map<ID, Collection<R>> queryResultsMap) {
        return newMap(queryResultsMap, map -> intersect(entityIds, map.keySet()).forEach(id -> map.put(id, List.of())));
    }
}
