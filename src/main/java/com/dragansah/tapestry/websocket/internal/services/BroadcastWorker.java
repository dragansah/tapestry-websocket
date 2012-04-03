package com.dragansah.tapestry.websocket.internal.services;

import org.apache.tapestry5.model.MutableComponentModel;
import org.apache.tapestry5.plastic.MethodAdvice;
import org.apache.tapestry5.plastic.MethodInvocation;
import org.apache.tapestry5.plastic.PlasticClass;
import org.apache.tapestry5.plastic.PlasticMethod;
import org.apache.tapestry5.services.Environment;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;
import org.apache.tapestry5.services.transform.TransformationSupport;

import com.dragansah.tapestry.websocket.annotations.Broadcast;

public class BroadcastWorker implements ComponentClassTransformWorker2
{
	private final Environment environment;

	public BroadcastWorker(Environment environment)
	{
		super();
		this.environment = environment;
	}

	@Override
	public void transform(PlasticClass plasticClass, TransformationSupport support,
			MutableComponentModel model)
	{
		for (PlasticMethod method : plasticClass.getMethodsWithAnnotation(Broadcast.class))
		{
			final String path = method.getAnnotation(Broadcast.class).path();
			method.addAdvice(new MethodAdvice()
			{
				@Override
				public void advise(MethodInvocation invocation)
				{
					environment.push(BroadcastPathInfo.class, new BroadcastPathInfo(path));
					invocation.proceed();
				}
			});
		}
	}
}
