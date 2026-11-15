package com.gudos.ui.rendering;

import java.awt.Graphics2D;
import java.util.Objects;

/** Decorator: is a SceneRenderer and holds another SceneRenderer, including other decorators. */
/*
 * RendererDecorator
 *
 * Purpose: Implements SceneRenderer while retaining another SceneRenderer and delegating to it.
 *
 * Execution flow: Concrete effect constructors call super(wrapped); render forwards the original
 * graphics/time/reducedEffects.
 *
 * Design rationale: Being the same component type and containing one component lets
 * decorators wrap decorators. Null rejection fails invalid chains early.
 *
 * State: wrapped is private final and nonnull; the base has no visual effect of its
 * own.
 *
 * Summary: Inheritance supplies shared delegation, and composition holds the wrapped
 * renderer. Both are visible here.
 */
public abstract class RendererDecorator implements SceneRenderer {
    private final SceneRenderer wrapped;

    /*
     * Require and retain one wrapped SceneRenderer. A wrapped decorator is valid because it
     * implements that same interface.
     */
    protected RendererDecorator(SceneRenderer wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped);
    }

    /*
     * Delegate exactly once to the wrapped SceneRenderer with the original graphics, time and
     * reducedEffects. A subclass calls super first, then adds its own layer; this preserves the base
     * room and supports nesting.
     */
    @Override public void render(Graphics2D graphics, double time, boolean reducedEffects) {
        wrapped.render(graphics, time, reducedEffects);
    }
}
