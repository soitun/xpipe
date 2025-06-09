package io.xpipe.core.dialog;

import io.xpipe.core.util.FailableConsumer;
import io.xpipe.core.util.FailableSupplier;
import io.xpipe.core.util.SecretValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A Dialog is a sequence of questions and answers.
 * <p>
 * The dialogue API is only used for the command line interface.
 * Therefore, the actual implementation is handled by the command line component.
 * This API provides a way of creating server-side dialogues which makes
 * it possible to create extensions that provide a commandline configuration component.
 * <p>
 * When a Dialog is completed, it can also be optionally evaluated to a value, which can be queried by calling {@link #getResult()}.
 * The evaluation function can be set with {@link #evaluateTo(Supplier)}.
 * Alternatively, a dialogue can also copy the evaluation function of another dialogue with {@link #evaluateTo(Dialog)}.
 * An evaluation result can also be mapped to another type with {@link #map(Function)}.
 * It is also possible to listen for the completion of this dialogue with {@link #onCompletion(FailableConsumer)}.
 */
public abstract class Dialog {

    private final List<FailableConsumer<?, Exception>> completion = new ArrayList<>();
    protected Object eval;
    private Supplier<?> evaluation;

    /**
     * Creates an empty dialogue. This dialogue completes immediately and does not handle any questions or answers.
     */
    public static Dialog empty() {
        return new Dialog() {
            @Override
            public DialogElement start() throws Exception {
                complete();
                return null;
            }

            @Override
            protected DialogElement next(String answer) throws Exception {
                complete();
                return null;
            }
        };
    }

    /**
     * Creates a choice dialogue.
     *
     * @param description the shown question description
     * @param elements    the available elements to choose from
     * @param required    signals whether a choice is required or can be left empty
     * @param selected    the selected element index
     */
    public static Dialog.Choice choice(
            String description,
            List<io.xpipe.core.dialog.Choice> elements,
            boolean required,
            boolean quiet,
            int selected) {
        Dialog.Choice c = new Dialog.Choice(description, elements, required, quiet, selected);
        c.evaluateTo(c::getSelected);
        return c;
    }

    /**
     * Creates a choice dialogue from a set of objects.
     *
     * @param description the shown question description
     * @param toString    a function that maps the objects to a string
     * @param required    signals whether choices required or can be left empty
     * @param def         the element which is selected by default
     * @param vals        the range of possible elements
     */
    @SafeVarargs
    public static <T> Dialog.Choice choice(
            String description, Function<T, String> toString, boolean required, boolean quiet, T def, T... vals) {
        var elements = Arrays.stream(vals)
                .map(v -> new io.xpipe.core.dialog.Choice(null, toString.apply(v)))
                .toList();
        var index = Arrays.asList(vals).indexOf(def);
        if (def != null && index == -1) {
            throw new IllegalArgumentException("Default value " + def + " is not in possible values");
        }

        var c = choice(description, elements, required, quiet, index);
        c.evaluateTo(() -> {
            if (c.getSelected() == -1) {
                return null;
            }
            return vals[c.getSelected()];
        });
        return c;
    }

    /**
     * Creates a simple query dialogue.
     *
     * @param description the shown question description
     * @param newLine     signals whether the query should be done on a new line or not
     * @param required    signals whether the query can be left empty or not
     * @param quiet       signals whether the user should be explicitly queried for the value.
     *                    In case the user is not queried, a value can still be set using the command line arguments
     *                    that allow to set the specific value for a configuration query parameter
     * @param value       the default value
     * @param converter   the converter
     */
    public static <T> Dialog.Query query(
            String description,
            boolean newLine,
            boolean required,
            boolean quiet,
            T value,
            QueryConverter<T> converter) {
        var q = new <T>Dialog.Query(description, newLine, required, quiet, value, converter, false);
        q.evaluateTo(q::getConvertedValue);
        return q;
    }

    /**
     * A special wrapper for secret values of {@link #query(String, boolean, boolean, boolean, Object, QueryConverter)}.
     */
    public static Dialog.Query querySecret(String description, boolean newLine, boolean required, SecretValue value) {
        var q = new Dialog.Query(description, newLine, required, false, value, QueryConverter.SECRET, true);
        q.evaluateTo(q::getConvertedValue);
        return q;
    }

    /**
     * Chains multiple dialogues together.
     *
     * @param ds the dialogues
     */
    public static Dialog chain(Dialog... ds) {
        return new Dialog() {

            private int current = 0;

            @Override
            public DialogElement start() throws Exception {
                current = 0;
                eval = null;
                DialogElement start;
                do {
                    start = ds[current].start();
                    if (start != null) {
                        return start;
                    }
                } while (++current < ds.length);

                current = ds.length - 1;
                return null;
            }

            @Override
            protected DialogElement next(String answer) throws Exception {
                DialogElement currentElement = ds[current].receive(answer);
                if (currentElement == null) {
                    DialogElement next = null;
                    while (current < ds.length - 1 && (next = ds[++current].start()) == null) {}
                    return next;
                }

                return currentElement;
            }
        }.evaluateTo(ds[ds.length - 1]);
    }

    /**
     * Creates a dialogue that starts from the beginning if the repeating condition is true.
     */
    public static <T> Dialog repeatIf(Dialog d, Predicate<T> shouldRepeat) {
        return new Dialog() {

            @Override
            public DialogElement start() throws Exception {
                eval = null;
                return d.start();
            }

            @Override
            protected DialogElement next(String answer) throws Exception {
                var next = d.receive(answer);
                if (next == null) {
                    if (shouldRepeat.test(d.getResult())) {
                        return d.start();
                    }
                }

                return next;
            }
        }.evaluateTo(d).onCompletion(d.completion);
    }

    /**
     * Create a simple dialogue that will print a message.
     */
    public static Dialog header(String msg) {
        return of(new HeaderElement(msg)).evaluateTo(() -> msg);
    }

    /**
     * Create a simple dialogue that will print a message.
     */
    public static Dialog header(Supplier<String> msg) {
        final String[] msgEval = {null};
        return new Dialog() {
            @Override
            public DialogElement start() {
                msgEval[0] = msg.get();
                return new HeaderElement(msgEval[0]);
            }

            @Override
            protected DialogElement next(String answer) {
                return null;
            }
        }.evaluateTo(() -> msgEval[0]);
    }

    /**
     * Creates a dialogue that will show a loading icon until the next dialogue question is sent.
     */
    public static Dialog busy() {
        return of(new BusyElement());
    }

    /**
     * Creates a dialogue that will only evaluate when needed.
     * This allows a dialogue to incorporate completion information about a previous dialogue.
     */
    public static Dialog lazy(FailableSupplier<Dialog> d) {
        return new Dialog() {

            Dialog dialog;

            @Override
            public DialogElement start() throws Exception {
                eval = null;
                dialog = d.get();
                var start = dialog.start();
                evaluateTo(dialog);
                if (start == null) {
                    complete();
                }
                return start;
            }

            @Override
            protected DialogElement next(String answer) throws Exception {
                return dialog.receive(answer);
            }
        };
    }

    private static Dialog of(DialogElement e) {
        return new Dialog() {

            @Override
            public DialogElement start() {
                eval = null;
                return e;
            }

            @Override
            protected DialogElement next(String answer) {
                if (e.apply(answer)) {
                    return null;
                }

                return e;
            }
        };
    }

    /**
     * Creates a dialogue that will not be executed if the condition is true.
     */
    public static Dialog skipIf(Dialog d, Supplier<Boolean> check) {
        return new Dialog() {

            private Dialog active;

            @Override
            public DialogElement start() throws Exception {
                active = check.get() ? null : d;
                if (active == null) {
                    complete();
                }

                return active != null ? active.start() : null;
            }

            @Override
            protected DialogElement next(String answer) throws Exception {
                return active != null ? active.receive(answer) : null;
            }
        }.evaluateTo(d).onCompletion(d.completion);
    }

    /**
     * Creates a dialogue that will repeat with an error message if the condition is met.
     */
    public static <T> Dialog retryIf(Dialog d, Function<T, String> msg) {
        return new Dialog() {

            private boolean retry;

            @Override
            public DialogElement start() throws Exception {
                eval = null;
                return d.start();
            }

            @Override
            protected DialogElement next(String answer) throws Exception {
                if (retry) {
                    retry = false;
                    return d.start();
                }

                var next = d.receive(answer);
                if (next == null) {
                    var s = msg.apply(d.getResult());
                    if (s != null) {
                        retry = true;
                        return new HeaderElement(s);
                    }
                }

                return next;
            }
        }.evaluateTo(d.evaluation).onCompletion(d.completion);
    }

    /**
     * Creates a dialogue that will fork the control flow.
     *
     * @param description the shown question description
     * @param elements    the available elements to choose from
     * @param required    signals whether a choice is required or not
     * @param selected    the index of the element that is selected by default
     * @param c           the dialogue index mapping function
     */
    public static Dialog fork(
            String description,
            List<io.xpipe.core.dialog.Choice> elements,
            boolean required,
            int selected,
            Function<Integer, Dialog> c) {
        var choice = new ChoiceElement(description, elements, required, false, selected);
        return new Dialog() {

            private Dialog choiceMade;

            {
                evaluateTo(() -> choiceMade);
            }

            @Override
            public DialogElement start() {
                choiceMade = null;
                eval = null;
                return choice;
            }

            @Override
            protected DialogElement next(String answer) throws Exception {
                if (choiceMade != null) {
                    return choiceMade.receive(answer);
                }

                if (choice.apply(answer)) {
                    choiceMade = c.apply(choice.getSelected());
                    return choiceMade != null ? choiceMade.start() : null;
                }

                return choice;
            }
        };
    }

    /**
     * Removes all completion listeners. Intended for internal use only.
     */
    public void clearCompletion() {
        completion.clear();
    }

    /* TODO: Implement automatic completion mechanism for start as well
     *   In case start returns null, the completion is not automatically done.
     * */
    public abstract DialogElement start() throws Exception;

    public Dialog evaluateTo(Dialog d) {
        evaluation = () -> d.evaluation != null ? d.evaluation.get() : null;
        return this;
    }

    public Dialog evaluateTo(Supplier<?> s) {
        evaluation = s;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> Dialog map(Function<T, ?> s) {
        var oldEval = evaluation;
        evaluation = () -> s.apply((T) oldEval.get());
        return this;
    }

    public Dialog onCompletion(FailableConsumer<?, Exception> s) {
        completion.add(s);
        return this;
    }

    public Dialog onCompletion(Runnable r) {
        completion.add(v -> r.run());
        return this;
    }

    public Dialog onCompletion(List<FailableConsumer<?, Exception>> s) {
        completion.addAll(s);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getResult() {
        return (T) eval;
    }

    @SuppressWarnings("unchecked")
    public <T> void complete() throws Exception {
        if (evaluation != null) {
            eval = evaluation.get();
            for (FailableConsumer<?, Exception> c : completion) {
                FailableConsumer<T, Exception> ct = (FailableConsumer<T, Exception>) c;
                ct.accept((T) eval);
            }
        }
    }

    public final DialogElement receive(String answer) throws Exception {
        var next = next(answer);
        if (next == null) {
            complete();
        }
        return next;
    }

    protected abstract DialogElement next(String answer) throws Exception;

    public static class Choice extends Dialog {

        private final ChoiceElement element;

        private Choice(
                String description,
                List<io.xpipe.core.dialog.Choice> elements,
                boolean required,
                boolean quiet,
                int selected) {
            this.element = new ChoiceElement(description, elements, required, quiet, selected);
        }

        @Override
        public DialogElement start() {
            return element;
        }

        @Override
        protected DialogElement next(String answer) {
            if (element.apply(answer)) {
                return null;
            }

            return element;
        }

        private int getSelected() {
            return element.getSelected();
        }
    }

    public static class Query extends Dialog {

        private final QueryElement element;

        private <T> Query(
                String description,
                boolean newLine,
                boolean required,
                boolean quiet,
                T value,
                QueryConverter<T> converter,
                boolean hidden) {
            this.element = new QueryElement(description, newLine, required, quiet, value, converter, hidden);
        }

        @Override
        public DialogElement start() {
            return element;
        }

        @Override
        protected DialogElement next(String answer) {
            if (element.requiresExplicitUserInput()
                    && (answer == null || answer.strip().length() == 0)) {
                return element;
            }

            if (element.apply(answer)) {
                return null;
            }

            return element;
        }

        private <T> T getConvertedValue() {
            return element.getConvertedValue();
        }
    }
}
