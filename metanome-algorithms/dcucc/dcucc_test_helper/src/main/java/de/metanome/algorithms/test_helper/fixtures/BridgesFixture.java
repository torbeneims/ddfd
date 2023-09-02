package de.metanome.algorithms.test_helper.fixtures;

import com.google.common.collect.ImmutableList;

import de.metanome.algorithm_integration.ColumnCombination;
import de.metanome.algorithm_integration.ColumnConditionAnd;
import de.metanome.algorithm_integration.ColumnConditionOr;
import de.metanome.algorithm_integration.ColumnConditionValue;
import de.metanome.algorithm_integration.ColumnIdentifier;
import de.metanome.algorithm_integration.input.InputGenerationException;
import de.metanome.algorithm_integration.input.InputIterationException;
import de.metanome.algorithm_integration.input.RelationalInputGenerator;
import de.metanome.algorithm_integration.result_receiver.ColumnNameMismatchException;
import de.metanome.algorithm_integration.result_receiver.ConditionalUniqueColumnCombinationResultReceiver;
import de.metanome.algorithm_integration.result_receiver.CouldNotReceiveResultException;
import de.metanome.algorithm_integration.result_receiver.FunctionalDependencyResultReceiver;
import de.metanome.algorithm_integration.result_receiver.InclusionDependencyResultReceiver;
import de.metanome.algorithm_integration.result_receiver.UniqueColumnCombinationResultReceiver;
import de.metanome.algorithm_integration.results.ConditionalUniqueColumnCombination;
import de.metanome.algorithm_integration.results.FunctionalDependency;
import de.metanome.backend.input.file.DefaultFileInputGenerator;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Jens Ehrlich
 */
public class BridgesFixture {

  protected ImmutableList<String>
      columnNames =
      ImmutableList.of("column1", "column2", "column3", "column4", "column5", "column6", "column7",
                       "column8", "column9", "column10", "column11", "column12", "column13");
  protected int numberOfColumns = 13;
  protected int rowPosition;
  protected String relationName = "bridges.csv";
  protected List<ImmutableList<String>> table = new LinkedList<>();
  protected FunctionalDependencyResultReceiver
      fdResultReceiver =
      mock(FunctionalDependencyResultReceiver.class);
  protected UniqueColumnCombinationResultReceiver
      uniqueColumnCombinationResultReceiver =
      mock(UniqueColumnCombinationResultReceiver.class);
  protected InclusionDependencyResultReceiver
      inclusionDependencyResultReceiver =
      mock(InclusionDependencyResultReceiver.class);
  protected ConditionalUniqueColumnCombinationResultReceiver
      cuccResultReceiver =
      mock(ConditionalUniqueColumnCombinationResultReceiver.class);

  public BridgesFixture() throws CouldNotReceiveResultException, ColumnNameMismatchException {
//        doAnswer(new Answer() {
//            public Object answer(InvocationOnMock invocation) {
//                Object[] args = invocation.getArguments();
//                System.out.println(args[0]);
//                return null;
//            }
//        }).when(fdResultReceiver).receiveResult(isA(FunctionalDependency.class));

//		doAnswer(new Answer() {
//			public Object answer(InvocationOnMock invocation) {
//				Object[] args = invocation.getArguments();
//				System.out.println(args[0]);
//				return null;
//			}
//		}).when(inclusionDependencyResultReceiver).receiveResult(isA(InclusionDependency.class));

//        doAnswer(new Answer() {
//            public Object answer(InvocationOnMock invocation) {
//                Object[] args = invocation.getArguments();
//                System.out.println(args[0]);
//                return null;
//            }
//        }).when(uccResultReceiver).receiveResult(isA(UniqueColumnCombination.class));

    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        System.out.println(args[0]);
        return null;
      }
    }).when(cuccResultReceiver).receiveResult(isA(ConditionalUniqueColumnCombination.class));

  }

  public RelationalInputGenerator getInputGenerator()
      throws InputGenerationException, InputIterationException, UnsupportedEncodingException,
             FileNotFoundException {
    String
        pathToInputFile =
        URLDecoder.decode(
            Thread.currentThread().getContextClassLoader().getResource(relationName).getPath(),
            "utf-8");
    RelationalInputGenerator
        inputGenerator =
        new DefaultFileInputGenerator(new File(pathToInputFile));
    return inputGenerator;
  }

  public FunctionalDependencyResultReceiver getFdResultReceiver() {
    return this.fdResultReceiver;
  }

  public UniqueColumnCombinationResultReceiver getUniqueColumnCombinationResultReceiver() {
    return this.uniqueColumnCombinationResultReceiver;
  }

  public InclusionDependencyResultReceiver getInclusionDependencyResultReceiver() {
    return this.inclusionDependencyResultReceiver;
  }

  public ConditionalUniqueColumnCombinationResultReceiver getCUCCResultReceiver() {
    return cuccResultReceiver;
  }

  public void verifyFunctionalDependencyResultReceiver() throws CouldNotReceiveResultException, ColumnNameMismatchException {
    ColumnIdentifier
        expectedIdentifier1 =
        new ColumnIdentifier(this.relationName, this.columnNames.get(0));
    ColumnIdentifier
        expectedIdentifier2 =
        new ColumnIdentifier(this.relationName, this.columnNames.get(1));
    ColumnIdentifier
        expectedIdentifier3 =
        new ColumnIdentifier(this.relationName, this.columnNames.get(2));
    ColumnIdentifier
        expectedIdentifier4 =
        new ColumnIdentifier(this.relationName, this.columnNames.get(3));
    ColumnIdentifier
        expectedIdentifier5 =
        new ColumnIdentifier(this.relationName, this.columnNames.get(4));
    ColumnIdentifier
        expectedIdentifier6 =
        new ColumnIdentifier(this.relationName, this.columnNames.get(5));
    ColumnIdentifier
        expectedIdentifier7 =
        new ColumnIdentifier(this.relationName, this.columnNames.get(6));
    ColumnIdentifier
        expectedIdentifier8 =
        new ColumnIdentifier(this.relationName, this.columnNames.get(7));
    ColumnIdentifier
        expectedIdentifier9 =
        new ColumnIdentifier(this.relationName, this.columnNames.get(8));
    ColumnIdentifier
        expectedIdentifier10 =
        new ColumnIdentifier(this.relationName, this.columnNames.get(9));
    ColumnIdentifier
        expectedIdentifier11 =
        new ColumnIdentifier(this.relationName, this.columnNames.get(10));
    ColumnIdentifier
        expectedIdentifier12 =
        new ColumnIdentifier(this.relationName, this.columnNames.get(11));
    ColumnIdentifier
        expectedIdentifier13 =
        new ColumnIdentifier(this.relationName, this.columnNames.get(12));

    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier1), expectedIdentifier2));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier1), expectedIdentifier3));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier1), expectedIdentifier4));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier1), expectedIdentifier5));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier1), expectedIdentifier6));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier1), expectedIdentifier7));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier1), expectedIdentifier8));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier1), expectedIdentifier9));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier1), expectedIdentifier10));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier1), expectedIdentifier11));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier1), expectedIdentifier12));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier1), expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier3), expectedIdentifier2));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier3, expectedIdentifier4),
                                 expectedIdentifier1));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier3, expectedIdentifier4),
                                 expectedIdentifier5));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier3, expectedIdentifier4),
                                 expectedIdentifier6));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier3, expectedIdentifier4),
                                 expectedIdentifier7));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier3, expectedIdentifier4),
                                 expectedIdentifier8));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier3, expectedIdentifier4),
                                 expectedIdentifier9));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier3, expectedIdentifier4),
                                 expectedIdentifier10));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier3, expectedIdentifier4),
                                 expectedIdentifier11));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier3, expectedIdentifier4),
                                 expectedIdentifier12));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier3, expectedIdentifier4),
                                 expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier4, expectedIdentifier6),
                                 expectedIdentifier2));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier4, expectedIdentifier6),
                                 expectedIdentifier7));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier4, expectedIdentifier6),
                                 expectedIdentifier8));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier4, expectedIdentifier6),
                                 expectedIdentifier9));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier4, expectedIdentifier6),
                                 expectedIdentifier10));
    verify(fdResultReceiver).receiveResult(
        new FunctionalDependency(new ColumnCombination(expectedIdentifier4, expectedIdentifier6),
                                 expectedIdentifier11));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier13, expectedIdentifier3, expectedIdentifier6),
        expectedIdentifier8));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier3, expectedIdentifier7),
        expectedIdentifier5));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier10, expectedIdentifier3, expectedIdentifier7),
        expectedIdentifier5));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier10, expectedIdentifier3, expectedIdentifier7),
        expectedIdentifier12));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier4, expectedIdentifier5, expectedIdentifier6),
        expectedIdentifier1));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier4, expectedIdentifier5, expectedIdentifier6),
        expectedIdentifier3));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier4, expectedIdentifier5, expectedIdentifier6),
        expectedIdentifier12));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier4, expectedIdentifier5, expectedIdentifier6),
        expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier2, expectedIdentifier4, expectedIdentifier5),
        expectedIdentifier10));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier10, expectedIdentifier11, expectedIdentifier6),
        expectedIdentifier8));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier3, expectedIdentifier6),
        expectedIdentifier8));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier12, expectedIdentifier4),
        expectedIdentifier10));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier13, expectedIdentifier4, expectedIdentifier6),
        expectedIdentifier1));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier13, expectedIdentifier4, expectedIdentifier6),
        expectedIdentifier3));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier13, expectedIdentifier4, expectedIdentifier6),
        expectedIdentifier5));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier13, expectedIdentifier4, expectedIdentifier6),
        expectedIdentifier12));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier10, expectedIdentifier3, expectedIdentifier6),
        expectedIdentifier8));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier10, expectedIdentifier3, expectedIdentifier6),
        expectedIdentifier12));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier13, expectedIdentifier4),
        expectedIdentifier9));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier4, expectedIdentifier5),
        expectedIdentifier10));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier4, expectedIdentifier6),
        expectedIdentifier1));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier4, expectedIdentifier6),
        expectedIdentifier3));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier4, expectedIdentifier6),
        expectedIdentifier5));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier4, expectedIdentifier6),
        expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier2, expectedIdentifier4),
        expectedIdentifier10));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier10, expectedIdentifier13, expectedIdentifier3),
        expectedIdentifier8));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier10, expectedIdentifier13, expectedIdentifier4,
                              expectedIdentifier7), expectedIdentifier5));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier13, expectedIdentifier3, expectedIdentifier5,
                              expectedIdentifier6), expectedIdentifier7));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier13, expectedIdentifier3, expectedIdentifier5,
                              expectedIdentifier6), expectedIdentifier10));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier13, expectedIdentifier3, expectedIdentifier5,
                              expectedIdentifier6), expectedIdentifier12));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier13, expectedIdentifier2,
                              expectedIdentifier4), expectedIdentifier5));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier13, expectedIdentifier3,
                              expectedIdentifier8), expectedIdentifier10));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier10, expectedIdentifier12, expectedIdentifier2,
                              expectedIdentifier6), expectedIdentifier8));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier10, expectedIdentifier13, expectedIdentifier2,
                              expectedIdentifier4), expectedIdentifier5));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier3, expectedIdentifier6, expectedIdentifier7,
                              expectedIdentifier9), expectedIdentifier5));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier3, expectedIdentifier6, expectedIdentifier7,
                              expectedIdentifier9), expectedIdentifier8));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier3, expectedIdentifier6, expectedIdentifier7,
                              expectedIdentifier9), expectedIdentifier10));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier3, expectedIdentifier6, expectedIdentifier7,
                              expectedIdentifier9), expectedIdentifier12));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier3, expectedIdentifier6, expectedIdentifier7,
                              expectedIdentifier9), expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier3, expectedIdentifier5,
                              expectedIdentifier6), expectedIdentifier10));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier10, expectedIdentifier5, expectedIdentifier6,
                              expectedIdentifier7), expectedIdentifier8));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier3, expectedIdentifier6,
                              expectedIdentifier7), expectedIdentifier5));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier13, expectedIdentifier3, expectedIdentifier6,
                              expectedIdentifier9), expectedIdentifier10));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier13, expectedIdentifier3, expectedIdentifier6,
                              expectedIdentifier9), expectedIdentifier12));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier2, expectedIdentifier4,
                              expectedIdentifier9), expectedIdentifier5));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier13, expectedIdentifier2,
                              expectedIdentifier6), expectedIdentifier12));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier3, expectedIdentifier7, expectedIdentifier8,
                              expectedIdentifier9), expectedIdentifier5));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier13, expectedIdentifier3,
                              expectedIdentifier6), expectedIdentifier10));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier13, expectedIdentifier3,
                              expectedIdentifier6), expectedIdentifier12));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier3, expectedIdentifier5, expectedIdentifier6,
                              expectedIdentifier8), expectedIdentifier10));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier3, expectedIdentifier5, expectedIdentifier6,
                              expectedIdentifier8), expectedIdentifier12));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier3, expectedIdentifier6, expectedIdentifier7,
                              expectedIdentifier8), expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier10, expectedIdentifier3, expectedIdentifier6,
                              expectedIdentifier7), expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier13, expectedIdentifier3,
                              expectedIdentifier7), expectedIdentifier5));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier3, expectedIdentifier6, expectedIdentifier8,
                              expectedIdentifier9), expectedIdentifier12));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier13, expectedIdentifier4,
                              expectedIdentifier7), expectedIdentifier5));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier13, expectedIdentifier3, expectedIdentifier7,
                              expectedIdentifier9), expectedIdentifier5));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier4, expectedIdentifier7,
                              expectedIdentifier8), expectedIdentifier5));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier10, expectedIdentifier12, expectedIdentifier6,
                              expectedIdentifier7), expectedIdentifier8));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier10, expectedIdentifier2, expectedIdentifier5,
                              expectedIdentifier6), expectedIdentifier8));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier13, expectedIdentifier3,
                              expectedIdentifier6), expectedIdentifier10));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier3, expectedIdentifier6,
                              expectedIdentifier8), expectedIdentifier10));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier3, expectedIdentifier6,
                              expectedIdentifier8), expectedIdentifier12));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier4, expectedIdentifier7,
                              expectedIdentifier9), expectedIdentifier5));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier12, expectedIdentifier3,
                              expectedIdentifier6), expectedIdentifier10));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier3, expectedIdentifier7,
                              expectedIdentifier8), expectedIdentifier5));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier13, expectedIdentifier3,
                              expectedIdentifier7), expectedIdentifier8));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier13, expectedIdentifier3,
                              expectedIdentifier7), expectedIdentifier10));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier3, expectedIdentifier6,
                              expectedIdentifier7), expectedIdentifier10));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier3, expectedIdentifier6,
                              expectedIdentifier7), expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier10, expectedIdentifier3, expectedIdentifier6,
                              expectedIdentifier9), expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier10, expectedIdentifier2, expectedIdentifier5,
                              expectedIdentifier6, expectedIdentifier7), expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier3, expectedIdentifier5, expectedIdentifier6,
                              expectedIdentifier8, expectedIdentifier9), expectedIdentifier7));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier3, expectedIdentifier5, expectedIdentifier6,
                              expectedIdentifier8, expectedIdentifier9), expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier2, expectedIdentifier4,
                              expectedIdentifier5, expectedIdentifier8), expectedIdentifier7));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier3, expectedIdentifier5,
                              expectedIdentifier6, expectedIdentifier9), expectedIdentifier7));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier3, expectedIdentifier5,
                              expectedIdentifier6, expectedIdentifier9), expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier2, expectedIdentifier5,
                              expectedIdentifier6, expectedIdentifier9), expectedIdentifier8));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier13, expectedIdentifier2,
                              expectedIdentifier4, expectedIdentifier8), expectedIdentifier7));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier2, expectedIdentifier4,
                              expectedIdentifier5, expectedIdentifier9), expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier13, expectedIdentifier2,
                              expectedIdentifier6, expectedIdentifier9), expectedIdentifier8));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier10, expectedIdentifier2, expectedIdentifier5,
                              expectedIdentifier6, expectedIdentifier9), expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier10, expectedIdentifier11, expectedIdentifier2,
                              expectedIdentifier5, expectedIdentifier6), expectedIdentifier12));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier13, expectedIdentifier2, expectedIdentifier4,
                              expectedIdentifier7, expectedIdentifier9), expectedIdentifier11));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier12, expectedIdentifier2,
                              expectedIdentifier4, expectedIdentifier9), expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier10, expectedIdentifier3, expectedIdentifier5,
                              expectedIdentifier6, expectedIdentifier9), expectedIdentifier7));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier13, expectedIdentifier2,
                              expectedIdentifier6, expectedIdentifier9), expectedIdentifier8));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier3, expectedIdentifier5,
                              expectedIdentifier6, expectedIdentifier9), expectedIdentifier7));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier3, expectedIdentifier5,
                              expectedIdentifier6, expectedIdentifier9), expectedIdentifier8));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier3, expectedIdentifier5,
                              expectedIdentifier6, expectedIdentifier9), expectedIdentifier10));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier3, expectedIdentifier5,
                              expectedIdentifier6, expectedIdentifier9), expectedIdentifier12));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier3, expectedIdentifier5,
                              expectedIdentifier6, expectedIdentifier9), expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier3, expectedIdentifier6,
                              expectedIdentifier8, expectedIdentifier9), expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier12, expectedIdentifier3,
                              expectedIdentifier6, expectedIdentifier9), expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier2, expectedIdentifier6,
                              expectedIdentifier7, expectedIdentifier9), expectedIdentifier8));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier12, expectedIdentifier13,
                              expectedIdentifier6, expectedIdentifier9), expectedIdentifier8));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier12, expectedIdentifier13, expectedIdentifier6,
                              expectedIdentifier7, expectedIdentifier9), expectedIdentifier8));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier2, expectedIdentifier5,
                              expectedIdentifier6, expectedIdentifier8), expectedIdentifier12));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier2, expectedIdentifier4,
                              expectedIdentifier8, expectedIdentifier9), expectedIdentifier7));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier10, expectedIdentifier11, expectedIdentifier12,
                              expectedIdentifier5, expectedIdentifier6, expectedIdentifier7),
        expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier10, expectedIdentifier13, expectedIdentifier5,
                              expectedIdentifier6, expectedIdentifier7, expectedIdentifier9),
        expectedIdentifier12));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier2, expectedIdentifier5,
                              expectedIdentifier6, expectedIdentifier8, expectedIdentifier9),
        expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier10, expectedIdentifier12, expectedIdentifier2,
                              expectedIdentifier6, expectedIdentifier7, expectedIdentifier9),
        expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier12, expectedIdentifier2,
                              expectedIdentifier5, expectedIdentifier6, expectedIdentifier9),
        expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier2, expectedIdentifier5, expectedIdentifier6,
                              expectedIdentifier7, expectedIdentifier8, expectedIdentifier9),
        expectedIdentifier12));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier10, expectedIdentifier11, expectedIdentifier13,
                              expectedIdentifier5, expectedIdentifier6, expectedIdentifier9),
        expectedIdentifier12));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier12, expectedIdentifier5,
                              expectedIdentifier6, expectedIdentifier7, expectedIdentifier8),
        expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier13, expectedIdentifier5, expectedIdentifier6,
                              expectedIdentifier7, expectedIdentifier8, expectedIdentifier9),
        expectedIdentifier12));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier10, expectedIdentifier2, expectedIdentifier5,
                              expectedIdentifier6, expectedIdentifier7, expectedIdentifier9),
        expectedIdentifier12));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier12, expectedIdentifier2,
                              expectedIdentifier5, expectedIdentifier6, expectedIdentifier7),
        expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier2, expectedIdentifier5,
                              expectedIdentifier6, expectedIdentifier7, expectedIdentifier8),
        expectedIdentifier13));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier11, expectedIdentifier13, expectedIdentifier5,
                              expectedIdentifier6, expectedIdentifier8, expectedIdentifier9),
        expectedIdentifier12));
    verify(fdResultReceiver).receiveResult(new FunctionalDependency(
        new ColumnCombination(expectedIdentifier13, expectedIdentifier2, expectedIdentifier6,
                              expectedIdentifier7, expectedIdentifier8, expectedIdentifier9),
        expectedIdentifier12));

    verifyNoMoreInteractions(fdResultReceiver);
  }

//	public void verifyUniqueColumnCombinationResultReceiver() throws CouldNotReceiveResultException {
//		ColumnIdentifier expectedIdentifierA = new ColumnIdentifier(this.relationName, this.columnNames.get(0));
//		ColumnIdentifier expectedIdentifierB = new ColumnIdentifier(this.relationName, this.columnNames.get(1));
//		ColumnIdentifier expectedIdentifierC = new ColumnIdentifier(this.relationName, this.columnNames.get(2));
//		ColumnIdentifier expectedIdentifierD = new ColumnIdentifier(this.relationName, this.columnNames.get(3));
//		ColumnIdentifier expectedIdentifierE = new ColumnIdentifier(this.relationName, this.columnNames.get(4));
//
//		//verify(uccResultReceiver).receiveResult(new UniqueColumnCombination(expectedIdentifierPROF));
//
//		verifyNoMoreInteractions(uccResultReceiver);
//	}

  public void verifyConditionalUniqueColumnCombination() throws CouldNotReceiveResultException, ColumnNameMismatchException {

    ColumnIdentifier
        c1 = new ColumnIdentifier(this.relationName, this.columnNames.get(0));
    ColumnIdentifier
        c2 = new ColumnIdentifier(this.relationName, this.columnNames.get(1));
    ColumnIdentifier
        c3 = new ColumnIdentifier(this.relationName, this.columnNames.get(2));
    ColumnIdentifier
        c4 = new ColumnIdentifier(this.relationName, this.columnNames.get(3));
    ColumnIdentifier
        c5 = new ColumnIdentifier(this.relationName, this.columnNames.get(4));
    ColumnIdentifier
        c6 = new ColumnIdentifier(this.relationName, this.columnNames.get(5));
    ColumnIdentifier
        c7 = new ColumnIdentifier(this.relationName, this.columnNames.get(6));
    ColumnIdentifier
        c8 = new ColumnIdentifier(this.relationName, this.columnNames.get(7));
    ColumnIdentifier
        c9 = new ColumnIdentifier(this.relationName, this.columnNames.get(8));
    ColumnIdentifier
        c10 = new ColumnIdentifier(this.relationName, this.columnNames.get(9));
    ColumnIdentifier
        c11 = new ColumnIdentifier(this.relationName, this.columnNames.get(10));
    ColumnIdentifier
        c12 = new ColumnIdentifier(this.relationName, this.columnNames.get(11));
    ColumnIdentifier
        c13 = new ColumnIdentifier(this.relationName, this.columnNames.get(12));

    verify(cuccResultReceiver).receiveResult(new ConditionalUniqueColumnCombination(
        new ColumnCombination(c3, c6),
        new ColumnConditionOr(new ColumnConditionAnd(new ColumnConditionValue(c10, "STEEL"),
                                                     new ColumnConditionValue(c11, "MEDIUM")))));
    verify(cuccResultReceiver).receiveResult(new ConditionalUniqueColumnCombination(
        new ColumnCombination(c3, c6),
        new ColumnConditionOr(new ColumnConditionAnd(new ColumnConditionValue(c10, "STEEL"),
                                                     new ColumnConditionValue(c13, "SIMPLE-T")))));
    verify(cuccResultReceiver).receiveResult(new ConditionalUniqueColumnCombination(
        new ColumnCombination(c3, c6),
        new ColumnConditionOr(new ColumnConditionAnd(new ColumnConditionValue(c10, "STEEL"),
                                                     new ColumnConditionValue(c9, "THROUGH")))));
    verify(cuccResultReceiver).receiveResult(new ConditionalUniqueColumnCombination(
        new ColumnCombination(c11, c12, c4, c5, c7),
        new ColumnConditionOr(new ColumnConditionAnd(new ColumnConditionValue(c2, "M"),
                                                     new ColumnConditionValue(c9, "THROUGH")))));
  }

  public void verifyConditionalUniqueColumnCombinationOr90() throws CouldNotReceiveResultException, ColumnNameMismatchException {

    ColumnIdentifier
        c1 = new ColumnIdentifier(this.relationName, this.columnNames.get(0));
    ColumnIdentifier
        c2 = new ColumnIdentifier(this.relationName, this.columnNames.get(1));
    ColumnIdentifier
        c3 = new ColumnIdentifier(this.relationName, this.columnNames.get(2));
    ColumnIdentifier
        c4 = new ColumnIdentifier(this.relationName, this.columnNames.get(3));
    ColumnIdentifier
        c5 = new ColumnIdentifier(this.relationName, this.columnNames.get(4));
    ColumnIdentifier
        c6 = new ColumnIdentifier(this.relationName, this.columnNames.get(5));
    ColumnIdentifier
        c7 = new ColumnIdentifier(this.relationName, this.columnNames.get(6));
    ColumnIdentifier
        c8 = new ColumnIdentifier(this.relationName, this.columnNames.get(7));
    ColumnIdentifier
        c9 = new ColumnIdentifier(this.relationName, this.columnNames.get(8));
    ColumnIdentifier
        c10 = new ColumnIdentifier(this.relationName, this.columnNames.get(9));
    ColumnIdentifier
        c11 = new ColumnIdentifier(this.relationName, this.columnNames.get(10));
    ColumnIdentifier
        c12 = new ColumnIdentifier(this.relationName, this.columnNames.get(11));
    ColumnIdentifier
        c13 = new ColumnIdentifier(this.relationName, this.columnNames.get(12));

    verify(cuccResultReceiver).receiveResult(new ConditionalUniqueColumnCombination(
        new ColumnCombination(c4, c6), new ColumnConditionOr(c2, "A", "M", "O")));

  }

  public void verifyConditionalUniqueColumnCombinationAndOr90()
      throws CouldNotReceiveResultException, ColumnNameMismatchException {
    ColumnIdentifier
        c1 = new ColumnIdentifier(this.relationName, this.columnNames.get(0));
    ColumnIdentifier
        c2 = new ColumnIdentifier(this.relationName, this.columnNames.get(1));
    ColumnIdentifier
        c3 = new ColumnIdentifier(this.relationName, this.columnNames.get(2));
    ColumnIdentifier
        c4 = new ColumnIdentifier(this.relationName, this.columnNames.get(3));
    ColumnIdentifier
        c5 = new ColumnIdentifier(this.relationName, this.columnNames.get(4));
    ColumnIdentifier
        c6 = new ColumnIdentifier(this.relationName, this.columnNames.get(5));
    ColumnIdentifier
        c7 = new ColumnIdentifier(this.relationName, this.columnNames.get(6));
    ColumnIdentifier
        c8 = new ColumnIdentifier(this.relationName, this.columnNames.get(7));
    ColumnIdentifier
        c9 = new ColumnIdentifier(this.relationName, this.columnNames.get(8));
    ColumnIdentifier
        c10 = new ColumnIdentifier(this.relationName, this.columnNames.get(9));
    ColumnIdentifier
        c11 = new ColumnIdentifier(this.relationName, this.columnNames.get(10));
    ColumnIdentifier
        c12 = new ColumnIdentifier(this.relationName, this.columnNames.get(11));
    ColumnIdentifier
        c13 = new ColumnIdentifier(this.relationName, this.columnNames.get(12));

    verify(cuccResultReceiver, times(0)).receiveResult(
        new ConditionalUniqueColumnCombination(new ColumnCombination(c11, c12, c13, c3, c7, c9),
                                               new ColumnConditionOr(
                                                   new ColumnConditionValue(c5, "AQUEDUCT"),
                                                   new ColumnConditionAnd(
                                                       new ColumnConditionValue(c2, "O"),
                                                       new ColumnConditionValue(c5, "HIGHWAY")),
                                                   new ColumnConditionAnd(
                                                       new ColumnConditionValue(c5, "RR"),
                                                       new ColumnConditionValue(c10, "WOOD"),
                                                       new ColumnConditionValue(c5, "HIGHWAY"),
                                                       new ColumnConditionValue(c10, "STEEL")),
                                                   new ColumnConditionAnd(
                                                       new ColumnConditionValue(c5, "RR"),
                                                       new ColumnConditionValue(c5, "HIGHWAY"),
                                                       new ColumnConditionValue(c8, "N"),
                                                       new ColumnConditionValue(c8, "G"))
                                               )));
//  verify(cuccResultReceiver, times(510)).receiveResult(
//      any(ConditionalUniqueColumnCombination.class));
  }
}