package unknow.sync;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import unknow.serialize.binary.BinaryFormat;
import unknow.serialize.binary.BinaryFormat.Builder;
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

/**
 * serialization utils
 * 
 * @author unknow
 */
public class Serialize {
	private static final BinaryFormat format;
	static {
		Builder builder = BinaryFormat.create();
		builder.register(LoginReq.class);
		builder.register(LoginRes.class);
		builder.register(GetFileDescs.class);
		builder.register(FileDesc[].class);
		builder.register(GetBlocReq.class);
		builder.register(DeleteReq.class);
		builder.register(GetFileReq.class);
		builder.register(StartAppend.class);
		builder.register(AppendData.class);
		builder.register(AppendBloc.class);
		builder.register(EndAppend.class);
		builder.register(Done.class);

		try {
			format = builder.build();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param output the output
	 * @param object the object
	 * @throws IOException
	 */
	public static void write(OutputStream output, Object object) throws IOException {
		format.write(object, output);
	}

	/**
	 * @param input the input
	 * @return the object
	 * @throws IOException
	 */
	public static Object read(InputStream input) throws IOException {
		return format.read(input);
	}
}
