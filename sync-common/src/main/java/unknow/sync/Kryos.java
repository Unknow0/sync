package unknow.sync;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;

import unknow.sync.proto.AppendBloc;
import unknow.sync.proto.AppendData;
import unknow.sync.proto.DeleteReq;
import unknow.sync.proto.EndAppend;
import unknow.sync.proto.GetBlocReq;
import unknow.sync.proto.GetFileDescs;
import unknow.sync.proto.GetFileReq;
import unknow.sync.proto.LoginReq;
import unknow.sync.proto.LoginRes;
import unknow.sync.proto.StartAppend;
import unknow.sync.proto.pojo.Done;
import unknow.sync.proto.pojo.FileDesc;

public class Kryos implements KryoFactory {
	private static final int STATIC_TRANSIANT = Modifier.STATIC | Modifier.TRANSIENT;
	private static final Set<Class<?>> register = new HashSet<>();
	static {
		addClass(LoginReq.class);
		addClass(LoginRes.class);
		addClass(GetFileDescs.class);
		addClass(FileDesc[].class);
		addClass(GetBlocReq.class);
		addClass(DeleteReq.class);
		addClass(GetFileReq.class);
		addClass(StartAppend.class);
		addClass(AppendData.class);
		addClass(AppendBloc.class);
		addClass(EndAppend.class);
		addClass(Done.class);
	}

	private static void addClass(Class<?> c) {
		if (c == Object.class || c == null || c == Enum.class || register.contains(c))
			return;
		register.add(c);
		if (c.isArray()) {
			addClass(c.getComponentType());
			return; // don't hash array
		}
		for (Field f : c.getDeclaredFields()) {
			if ((f.getModifiers() & STATIC_TRANSIANT) != 0)
				continue;
			addClass(f.getType());
		}
		addClass(c.getSuperclass());
	}

	private final KryoPool pool = new KryoPool.Builder(this).build();

	@Override
	public Kryo create() {
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(true);
		for (Class<?> c : register)
			kryo.register(c);
		return kryo;
	}

	public void write(Output output, Object object) {
		Kryo kryo = pool.borrow();
		try {
			kryo.writeClassAndObject(output, object);
			output.flush();
		} finally {
			pool.release(kryo);
		}
	}

	public Object read(Input input) {
		Kryo kryo = pool.borrow();
		try {
			return kryo.readClassAndObject(input);
		} finally {
			pool.release(kryo);
		}
	}
}
