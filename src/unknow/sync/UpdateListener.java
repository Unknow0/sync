package unknow.sync;

public interface UpdateListener
	{

	public void startUpdate(String project, int size);

	public void doneUpdate(String project);

	public void startCheckFile(CharSequence name, int blocCount);

	public void updateCheck(CharSequence name, float rate);

	public void doneCheckFile(CharSequence name, int missingBloc);

	public void updateReconstruct(CharSequence name, float rate);

	public void doneReconstruct(CharSequence name, boolean ok);

	public void doneFile(CharSequence name);
	}
