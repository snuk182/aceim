package aceim.app.utils.linq;

import java.util.Collection;

public final class KindaLinq {

	public static <T> KindaLinqQuery<T> from(Collection<T> collection) {
		return new KindaLinqQuery<T>(collection);
	}
}
