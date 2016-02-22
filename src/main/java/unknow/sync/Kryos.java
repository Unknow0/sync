package unknow.sync;

import java.security.*;

import unknow.common.kryo.*;
import unknow.sync.proto.*;
import unknow.sync.proto.pojo.*;

import com.esotericsoftware.kryo.*;

public class Kryos extends KryoWrap
	{
	public Kryos() throws NoSuchAlgorithmException
		{
		super();
		this.addClass(String.class);
		this.addClass(LoginReq.class);
		this.addClass(LoginRes.class);
		this.addClass(GetFileDescs.class);
		this.addClass(FileDesc[].class);
		this.addClass(GetBlocReq.class);
		this.addClass(byte[].class);
		this.addClass(DeleteReq.class);
		this.addClass(boolean.class);
		this.addClass(GetFileReq.class);
		this.addClass(StartAppend.class);
		this.addClass(AppendData.class);
		this.addClass(AppendBloc.class);
		this.addClass(EndAppend.class);
		this.addClass(Done.class);
		this.doneInit();
		}

	protected void init(Kryo kryo)
		{
		super.init(kryo);
		}
	}
