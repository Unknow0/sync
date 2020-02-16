package unknow.sync;

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
	private final KryoPool pool = new KryoPool.Builder(this).build();

	@Override
	public Kryo create() {
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(true);
		kryo.register(LoginReq.class);
		kryo.register(LoginRes.class);
		kryo.register(GetFileDescs.class);
		kryo.register(FileDesc[].class);
		kryo.register(GetBlocReq.class);
		kryo.register(byte[].class);
		kryo.register(DeleteReq.class);
		kryo.register(boolean.class);
		kryo.register(GetFileReq.class);
		kryo.register(StartAppend.class);
		kryo.register(AppendData.class);
		kryo.register(AppendBloc.class);
		kryo.register(EndAppend.class);
		kryo.register(Done.class);
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
