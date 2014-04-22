/*
 * Copyright 2014 scalikejdbc.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package scalikejdbc

/**
 * BigDecimal converter.
 * @param bd big decimal value
 */
class ScalaBigDecimalConverter(val value: java.math.BigDecimal) extends AnyVal {

  def toScalaBigDecimal: scala.math.BigDecimal = {
    if (value == null) null.asInstanceOf[scala.math.BigDecimal]
    else new scala.math.BigDecimal(value)
  }

}
