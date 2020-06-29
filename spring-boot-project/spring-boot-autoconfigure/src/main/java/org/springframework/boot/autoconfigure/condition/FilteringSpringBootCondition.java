/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * Abstract base class for a {@link SpringBootCondition} that also implements
 * {@link AutoConfigurationImportFilter}.
 *
 * @author Phillip Webb
 */
abstract class FilteringSpringBootCondition extends SpringBootCondition
		implements AutoConfigurationImportFilter, BeanFactoryAware, BeanClassLoaderAware {

	private BeanFactory beanFactory;

	private ClassLoader beanClassLoader;

	@Override
	public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
		// 获得 ConditionEvaluationReport 对象
		ConditionEvaluationReport report = ConditionEvaluationReport.find(this.beanFactory);
		// 执行批量的匹配，并返回匹配结果
		ConditionOutcome[] outcomes = getOutcomes(autoConfigurationClasses, autoConfigurationMetadata);
		// 创建match数组
		boolean[] match = new boolean[outcomes.length];
		// 遍历outcomes数组
		for (int i = 0; i < outcomes.length; i++) {
			// 如果返回为空，也认为匹配
			match[i] = (outcomes[i] == null || outcomes[i].isMatch());
			if (!match[i] && outcomes[i] != null) {
				// 打印日志
				logOutcome(autoConfigurationClasses[i], outcomes[i]);
				// 记录
				if (report != null) {
					report.recordConditionEvaluation(autoConfigurationClasses[i], this, outcomes[i]);
				}
			}
		}
		// 返回结果
		return match;
	}

	protected abstract ConditionOutcome[] getOutcomes(String[] autoConfigurationClasses,
													  AutoConfigurationMetadata autoConfigurationMetadata);

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	protected final BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	protected final ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	protected List<String> filter(Collection<String> classNames, ClassNameFilter classNameFilter,
								  ClassLoader classLoader) {
		// 如果为空，则返回空结果
		if (CollectionUtils.isEmpty(classNames)) {
			return Collections.emptyList();
		}
		// 遍历 classNames 数组，使用 ClassNameFilter 进行判断，是否匹配。
		List<String> matches = new ArrayList<>(classNames.size());
		for (String candidate : classNames) {
			if (classNameFilter.matches(candidate, classLoader)) {
				matches.add(candidate);
			}
		}
		return matches;
	}

	protected enum ClassNameFilter {

		PRESENT {
			@Override
			public boolean matches(String className, ClassLoader classLoader) {
				return isPresent(className, classLoader);
			}

		},

		MISSING {
			@Override
			public boolean matches(String className, ClassLoader classLoader) {
				// 其实这里就是去加载类，看是否成功，注意这里是missing，所以如果isPresent为true表示类加载成功，需返回false
				// 如果类未加载成功，抛出异常，返回false，然后再返回true
				// 稍微有一点绕，注意 因为是missing，所以未匹配成功返回true，成功匹配返回false
				return !isPresent(className, classLoader);
			}

		};

		public abstract boolean matches(String className, ClassLoader classLoader);

		public static boolean isPresent(String className, ClassLoader classLoader) {
			// 成功加载返回true，失败返回false 
			// 但是对应MISSING和PRESENT最终结果是反的
			if (classLoader == null) {
				classLoader = ClassUtils.getDefaultClassLoader();
			}
			try {
				forName(className, classLoader);
				return true;
			} catch (Throwable ex) {
				// 通过异常的方式来返回false
				return false;
			}
		}

		private static Class<?> forName(String className, ClassLoader classLoader) throws ClassNotFoundException {
			if (classLoader != null) {
				return classLoader.loadClass(className);
			}
			return Class.forName(className);
		}

	}

}
