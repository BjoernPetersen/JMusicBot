package com.github.bjoernpetersen.jmusicbot.provider;

import com.github.bjoernpetersen.jmusicbot.InitStateWriter;
import com.github.bjoernpetersen.jmusicbot.PlaybackFactoryManager;
import com.github.bjoernpetersen.jmusicbot.Plugin.State;
import com.github.bjoernpetersen.jmusicbot.PluginWrapper;
import com.github.bjoernpetersen.jmusicbot.Reference;
import com.github.bjoernpetersen.jmusicbot.config.Config;
import java.io.Closeable;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ProviderManager extends Closeable {

  void initialize(@Nonnull Config config, @Nonnull PlaybackFactoryManager manager);

  /**
   * Gets a map from provider ID to provider with all providers, regardless of state.
   *
   * @return a map (providerId -> provider)
   */
  @Nonnull
  Map<String, ? extends ProviderWrapper> getAllProviders();

  /**
   * Gets a map from suggester ID to suggester with all suggesters, regardless of state.
   *
   * @return a map (suggesterId -> suggester)
   */
  @Nonnull
  Map<String, ? extends SuggesterWrapper> getAllSuggesters();

  /**
   * Gets all active suggesters for the specified provider.
   *
   * @param provider a Provider
   * @return a collection of suggesters
   */
  @Nonnull
  Collection<? extends Suggester> getSuggesters(@Nonnull Provider provider);

  /**
   * Initializes all providers that are currently in the {@link State#CONFIG} state.
   *
   * @param initStateWriter an InitStateWriter
   */
  void initializeProviders(@Nonnull InitStateWriter initStateWriter);

  /**
   * Initializes all suggesters that are currently in the {@link State#CONFIG} state.
   *
   * @param initStateWriter an InitStateWriter
   */
  void initializeSuggesters(@Nonnull InitStateWriter initStateWriter);

  /**
   * Gets all active providers.
   *
   * @return a stream of providers
   */
  @Nonnull
  default Stream<? extends Provider> getProviders() {
    return getAllProviders().values().stream()
        .filter(PluginWrapper::isActive);
  }

  /**
   * Gets all active suggesters.
   *
   * @return a stream of suggesters
   */
  @Nonnull
  default Stream<? extends Suggester> getSuggesters() {
    return getAllSuggesters().values().stream()
        .filter(PluginWrapper::isActive);
  }

  /**
   * Gets the provider with the specified name. Only returns active providers.
   *
   * @param id the provider ID
   * @return a Provider
   * @throws IllegalArgumentException if there is no such provider
   */
  @Nullable
  ProviderWrapper getProvider(@Nonnull String id);

  /**
   * Gets the suggester with the specified name. Only returns active suggesters.
   *
   * @param id the suggester ID
   * @return a Suggester
   * @throws IllegalArgumentException if there is no such suggester
   */
  @Nullable
  SuggesterWrapper getSuggester(@Nonnull String id);

  @Nonnull
  default ProviderWrapper getWrapper(@Nonnull Provider provider) {
    if (provider instanceof ProviderWrapper) {
      return (ProviderWrapper) provider;
    } else {
      ProviderWrapper result = getProvider(provider.getId());
      if (result == null) {
        throw new IllegalArgumentException("Provider not found: " + provider.getId());
      }
      return result;
    }
  }

  @Nonnull
  default SuggesterWrapper getWrapper(@Nonnull Suggester suggester) {
    if (suggester instanceof SuggesterWrapper) {
      return (SuggesterWrapper) suggester;
    } else {
      SuggesterWrapper result = getSuggester(suggester.getId());
      if (result == null) {
        throw new IllegalArgumentException("Provider not found: " + suggester.getId());
      }
      return result;
    }
  }

  interface ProviderWrapper extends PluginWrapper<Provider>, Provider {

    Reference<Function<Provider, ProviderWrapper>> factoryRef =
        new Reference<>(DefaultProviderWrapper::new);

    static void setDefaultFactory(@Nonnull Function<Provider, ProviderWrapper> factory) {
      ProviderWrapper.factoryRef.set(factory);
    }

    static ProviderWrapper defaultWrapper(@Nonnull Provider provider) {
      return factoryRef.get().apply(provider);
    }
  }

  interface SuggesterWrapper extends PluginWrapper<Suggester>, Suggester {

    Reference<Function<Suggester, SuggesterWrapper>> factoryRef =
        new Reference<>(DefaultSuggesterWrapper::new);

    static void setDefaultFactory(@Nonnull Function<Suggester, SuggesterWrapper> factory) {
      SuggesterWrapper.factoryRef.set(factory);
    }

    static SuggesterWrapper defaultWrapper(@Nonnull Suggester suggester) {
      return factoryRef.get().apply(suggester);
    }
  }

  static ProviderManager defaultManager() {
    return new DefaultProviderManager();
  }
}
