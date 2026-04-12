package me.suwash.tools.comparefiles.infra.exception;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * spring-hateoas VndErrors の最小互換クラス。
 * Spring Boot 3.x で VndErrors が削除されたため、プロジェクト内に置き換えとして定義。
 */
public class VndErrors implements Iterable<VndErrors.VndError> {

    private final List<VndError> errors = new ArrayList<>();

    public VndErrors() {
    }

    public VndErrors(final String logref, final String message) {
        errors.add(new VndError(logref, message));
    }

    public VndErrors(final VndError error) {
        errors.add(error);
    }

    public VndErrors(final List<VndError> errors) {
        this.errors.addAll(errors);
    }

    public VndErrors add(final VndError error) {
        errors.add(error);
        return this;
    }

    @Override
    public Iterator<VndError> iterator() {
        return errors.iterator();
    }

    @Override
    public int hashCode() {
        return errors.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return errors.equals(((VndErrors) obj).errors);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (final VndError error : errors) {
            sb.append(error.getLogref()).append(':').append(error.getMessage()).append('\n');
        }
        return sb.toString();
    }

    /**
     * エラー要素。
     */
    public static class VndError {

        private final String logref;
        private final String message;

        public VndError(final String logref, final String message) {
            this.logref = logref;
            this.message = message;
        }

        public String getLogref() {
            return logref;
        }

        public String getMessage() {
            return message;
        }
    }
}
