package com.itc.funkart.gateway.config;

import java.lang.annotation.*;

/**
 * <h2>Exclusion Marker for Global API Versioning</h2>
 * <p>
 * This annotation is used to signal the {@link WebConfig} to bypass the
 * automatic path prefixing (e.g., {@code /api/v1}) for a specific Controller.
 * </p>
 * <h3>Usage Scenarios:</h3>
 * <ul>
 * <li><b>Actuator Endpoints:</b> Monitoring tools often expect health checks at {@code /health} rather than {@code /api/v1/health}.</li>
 * <li><b>OAuth2 Callbacks:</b> Social login providers (like GitHub) require static redirect URIs that may not follow versioning patterns.</li>
 * <li><b>Static Resources:</b> If the Gateway serves static assets or simple landing pages.</li>
 * </ul>
 * * <p>
 * Apply this at the <b>Class level</b>. If present, the Gateway will serve the
 * endpoints using only the path defined in the {@code @RequestMapping}.
 * </p>
 * * @see WebConfig#configurePathMatching
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NoApiPrefix {
}